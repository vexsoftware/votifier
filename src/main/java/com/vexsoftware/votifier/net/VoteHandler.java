package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.crypto.RSA;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Joe Hirschfeld on 10/16/2015.
 */
public class VoteHandler extends Thread {

    private static final Logger LOG = Logger.getLogger("Votifier");

    private static final char[] OK = "OK\n".toCharArray();
    private static final char[] BAD = "BAD\n".toCharArray();

    public VoteHandler(Socket socket, VoteReceiver receiver) {
        this.socket = socket;
        this.receiver = receiver;
    }

    private final Socket socket;
    private final VoteReceiver receiver;

    @Override
    public void run() {

        int length;
        byte[] block = new byte[256];

        BufferedWriter writer = null;
        InputStream in = null;

        InetAddress remoteAddress = VoteReceiver.getRemoteAddress(socket);
        try {
            socket.setSoTimeout(5000);

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = socket.getInputStream();

            writer.write("VOTIFIER " + Votifier.getInstance().getVersion());
            writer.newLine();
            writer.flush();

            length = in.read(block, 0, block.length);

            if(length != block.length){
                LOG.log(Level.WARNING,"Illegal Length packet sent. Expected: 256 Received: "+length);
                bad(writer);
                receiver.countBadPacket(remoteAddress);
                return;
            }

            try {
                block = RSA.decrypt(block, Votifier.getInstance().getKeyPair().getPrivate());
            } catch (BadPaddingException e) {
                LOG.log(Level.WARNING, "RSA Exception. Make sure that your public key \n matches the one you gave on the server list.");
                bad(writer);
                receiver.countBadPacket(remoteAddress);
                return;
            } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException e) {
                LOG.log(Level.WARNING, "RSA Exception, ignoring packet - " + e.getLocalizedMessage());
                bad(writer);
                receiver.countBadPacket(remoteAddress);
                return;
            }

            ByteBuffer data = ByteBuffer.wrap(block, 0, block.length); //Chop off empty data

            String opcode = readString(data);

            if (!"VOTE".equals(opcode)) {
                LOG.log(Level.WARNING, "RSA Decode did not produce correct opcode.");
                bad(writer);
                return;
            }

            String serviceName = readString(data);
            String username = readString(data);
            String address = readString(data);
            String timeStamp = readString(data);

            ok(writer);
            receiver.badPacketCounter.remove(remoteAddress);

            final Vote v = new Vote(serviceName, username, address, timeStamp);

            if (Votifier.getInstance().isDebug())
                LOG.info("Received vote record -> " + v);

            for (VoteListener listener : Votifier.getInstance().getListeners()) {
                try {
                    listener.voteMade(v);
                } catch (Exception e) {
                    String vlName = listener.getClass().getSimpleName();
                    LOG.log(Level.WARNING, "Exception caught while sending the vote notification to the '" + vlName + "' listener", e);
                }
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(Votifier.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().getPluginManager().callEvent(new VotifierEvent(v));
                }
            });
        } catch (SocketException e) {
            LOG.log(Level.WARNING, "Protocol error. Ignoring packet - " + e.getLocalizedMessage() + " - Remote IP: "+remoteAddress.getHostAddress());
            receiver.countBadPacket(remoteAddress);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "IO Exception, ignoring packet - " + e.getLocalizedMessage() + " - Remote IP: " + remoteAddress.getHostAddress());
            receiver.countBadPacket(remoteAddress);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Something unknown occured while decoding a vote packet! - " + e.getLocalizedMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String readString(ByteBuffer buf) {
        StringBuilder ret = new StringBuilder();
        while (buf.hasRemaining()) {
            byte curr = buf.get();
            if (curr == '\n')
                break;
            ret.append((char)curr);
        }
        return ret.toString();
    }

    /**
     * Sends a 'bad' signal through a writer, but does not close it.
     * @param writer writer for the bad signal to be sent through
     * @throws IOException if the underlying writer throws an IOExceptions
     */
    public static void bad(Writer writer) throws IOException {
        writer.write(BAD);
        writer.flush();
    }

    /**
     * Sends an 'ok' signal through a writer, but does not close it.
     * @param writer writer for the bad signal to be sent through
     * @throws IOException if the underlying writer throws an IOExceptions
     */
    public static void ok(Writer writer) throws IOException {
        writer.write(OK);
        writer.flush();
    }
}
