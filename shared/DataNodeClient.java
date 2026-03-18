package shared;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * Simple client for communicating with a Data Node.
 *
 * Protocol:
 *   - STORE_CHUNK: client sends a UTF header string followed by raw bytes
 *   - GET_CHUNK: client sends a UTF header string and reads back a UTF response + raw bytes
 */
public class DataNodeClient {

    private final String host;
    private final int port;

    public DataNodeClient(String host, int port) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
    }

    /**
     * Store a chunk on the data node.
     */
    public boolean storeChunk(int chunkId, int chunkIndex, int fileId, String fileName, byte[] chunkBytes) {
        if (chunkBytes == null) {
            throw new IllegalArgumentException("chunkBytes cannot be null");
        }

        String header = String.format("STORE_CHUNK|%d|%d|%d|%d|%s", chunkId, chunkIndex, fileId, chunkBytes.length, fileName);

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Send header as UTF which includes length prefix
            out.writeUTF(header);
            // Send payload
            out.write(chunkBytes);
            out.flush();

            String response = in.readUTF();
            return response != null && response.startsWith("OK");

        } catch (IOException e) {
            System.err.println("✗ Failed to store chunk " + chunkId + " on " + host + ":" + port + " -> " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve a chunk from the data node.
     * @return raw bytes or null if failed
     */
    public byte[] getChunk(int chunkId) {
        String header = String.format("GET_CHUNK|%d", chunkId);

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(header);
            out.flush();

            String response = in.readUTF();
            if (response == null || !response.startsWith("OK|")) {
                System.err.println("✗ Unexpected response from node: " + response);
                return null;
            }

            String[] parts = response.split("\\|", 2);
            if (parts.length < 2) {
                System.err.println("✗ Invalid response format from node: " + response);
                return null;
            }

            int expectedSize;
            try {
                expectedSize = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("✗ Invalid size in response: " + parts[1]);
                return null;
            }

            byte[] buffer = new byte[expectedSize];
            in.readFully(buffer);
            return buffer;

        } catch (IOException e) {
            System.err.println("✗ Failed to download chunk " + chunkId + " from " + host + ":" + port + " -> " + e.getMessage());
            return null;
        }
    }
}
