package servlet;

import dao.ChunkDAO;
import dao.ChunkLocationDAO;
import dao.FileDAO;
import dao.NodeDAO;
import shared.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Streams file bytes with the correct Content-Type so the browser
 * renders them inline (images, PDF, video, audio, plain text/code).
 * Non-previewable types fall back to a normal download.
 *
 * URL: /preview?file_id=<id>
 */
@WebServlet("/preview")
public class PreviewServlet extends HttpServlet {

    private FileDAO          fileDAO;
    private ChunkDAO         chunkDAO;
    private ChunkLocationDAO chunkLocationDAO;
    private NodeDAO          nodeDAO;

    @Override
    public void init() throws ServletException {
        fileDAO          = new FileDAO();
        chunkDAO         = new ChunkDAO();
        chunkLocationDAO = new ChunkLocationDAO();
        nodeDAO          = new NodeDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User user = (User) session.getAttribute("user");

        String fileIdParam = request.getParameter("file_id");
        if (fileIdParam == null || fileIdParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file_id");
            return;
        }

        int fileId;
        try { fileId = Integer.parseInt(fileIdParam.trim()); }
        catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file_id");
            return;
        }

        FileMetadata file = fileDAO.getFileById(fileId);
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }
        if (file.getOwnerId() != user.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        List<Chunk> chunks = chunkDAO.getChunksByFileId(fileId);
        if (chunks == null || chunks.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No chunks found");
            return;
        }

        String mimeType   = resolveMime(file.getFileName());
        boolean isInline  = isInlineType(mimeType);

        response.setContentType(mimeType);
        response.setContentLengthLong(file.getFileSize());
        response.setHeader("Cache-Control", "private, max-age=300");

        String encodedName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        String disposition = isInline
                ? "inline; filename=\"" + file.getFileName() + "\"; filename*=UTF-8''" + encodedName
                : "attachment; filename=\"" + file.getFileName() + "\"; filename*=UTF-8''" + encodedName;
        response.setHeader("Content-Disposition", disposition);

        OutputStream out = response.getOutputStream();
        for (Chunk chunk : chunks) {
            List<ChunkLocation> locations = chunkLocationDAO.getChunkLocations(chunk.getChunkId());
            if (locations == null || locations.isEmpty()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Chunk location missing");
                return;
            }
            byte[] data = null;
            for (ChunkLocation loc : locations) {
                NodeInfo node = nodeDAO.getNodeById(loc.getNodeId());
                if (node == null || !"ACTIVE".equalsIgnoreCase(node.getStatus())) continue;
                DataNodeClient client = new DataNodeClient(node.getIpAddress(), node.getPort());
                data = client.getChunk(chunk.getChunkId());
                if (data != null) break;
            }
            if (data == null) { out.flush(); return; }
            out.write(data);
        }
        out.flush();
    }

    /** Map filename extension to MIME type. */
    private static String resolveMime(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String n = fileName.toLowerCase();
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".gif"))  return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".svg"))  return "image/svg+xml";
        if (n.endsWith(".ico"))  return "image/x-icon";
        if (n.endsWith(".bmp"))  return "image/bmp";
        if (n.endsWith(".pdf"))  return "application/pdf";
        if (n.endsWith(".mp4"))  return "video/mp4";
        if (n.endsWith(".webm")) return "video/webm";
        if (n.endsWith(".ogv"))  return "video/ogg";
        if (n.endsWith(".mov"))  return "video/quicktime";
        if (n.endsWith(".mp3"))  return "audio/mpeg";
        if (n.endsWith(".wav"))  return "audio/wav";
        if (n.endsWith(".ogg"))  return "audio/ogg";
        if (n.endsWith(".flac")) return "audio/flac";
        if (n.endsWith(".txt"))  return "text/plain;charset=UTF-8";
        if (n.endsWith(".md"))   return "text/plain;charset=UTF-8";
        if (n.endsWith(".csv"))  return "text/plain;charset=UTF-8";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".xml"))  return "text/xml;charset=UTF-8";
        if (n.endsWith(".html") || n.endsWith(".htm")) return "text/plain;charset=UTF-8"; // plain to avoid XSS
        if (n.endsWith(".java") || n.endsWith(".py")  || n.endsWith(".js")
         || n.endsWith(".ts")   || n.endsWith(".css") || n.endsWith(".sh")
         || n.endsWith(".sql")  || n.endsWith(".yaml")|| n.endsWith(".yml")
         || n.endsWith(".toml") || n.endsWith(".ini") || n.endsWith(".env")
         || n.endsWith(".c")    || n.endsWith(".cpp") || n.endsWith(".h"))
            return "text/plain;charset=UTF-8";
        return "application/octet-stream";
    }

    /** Returns true if the browser should render this inline vs force-download. */
    private static boolean isInlineType(String mime) {
        if (mime == null) return false;
        return mime.startsWith("image/")
            || mime.startsWith("video/")
            || mime.startsWith("audio/")
            || mime.startsWith("text/")
            || mime.equals("application/pdf")
            || mime.equals("application/json");
    }
}