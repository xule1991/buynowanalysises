import com.jcraft.jsch.*;

public class JSchUtil {
    JSch jSch = new JSch();
    Session session = null;
    ChannelSftp channelSftp = null;

    JSchUtil(String appname, String ip, int port, String password) throws JSchException {
        session = jSch.getSession(appname, ip, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);   // making a connection with timeout.
        System.out.println("Session connected.");
        System.out.println("Opening Channel.");
        channelSftp = (ChannelSftp)session.openChannel("sftp");
        channelSftp.connect();
        System.out.println("Channel connected");

    }

    public void put(String src, String dst){
        try {
            channelSftp.put(src, dst);
        } catch (SftpException e) {
            System.out.println("put error");
            e.printStackTrace();
        }
    }

    public void get(String src, String dst){
        try {
            channelSftp.get(src, dst);
        } catch (SftpException e) {
            System.out.println("get error");
            e.printStackTrace();
        }
    }
    public void ls() throws SftpException {
        channelSftp.ls("*.txt");
    }
}
