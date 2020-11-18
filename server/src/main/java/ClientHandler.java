import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private UserInfo user;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            doAuth(in, out);
            File rootDir = new File("server/" + user.getLogin() + "/");
            out.writeUTF(rootDir.getPath());
            out.flush();

            while (true) {
                String command = in.readUTF();

                // получение информации для обновления таблицы
                if (command.startsWith("info")) {
                    String path = command.replace("info", "");
                    System.out.println("path :" + path + ".");
                    File file = new File("./" + path);
                    if (file.exists()) {
                        out.writeObject(makeFileInfo(Paths.get(path)));
                        out.flush();
                    } else System.out.println("Attention. There ia failure to find the desired file or folder.");
                }
                // копирование с клиента на сервер
                if (command.equals("upload")) {
                    try {
                        String path = in.readUTF();
                        File file = new File(path);
                        System.out.println("path: " + path);
                        if (!file.exists()) {
                            file.createNewFile();
                        } //TODO отработать на случай замены файла
                        long size = in.readLong();
                        System.out.println(size);
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buffer = new byte[256];
                        for (int i = 0; i < (size + 255) / 256; i++) {
                            int read = in.read(buffer);
                            fos.write(buffer, 0, read);
                            System.out.println("reading...");
                        }
                        fos.close();
                        out.writeUTF("File is copied.");
                        out.flush();
                    } catch (Exception e) {
                        out.writeUTF("WRONG");
                    }
                }

                // копирование с сервера на клиент
                if (command.equals("download")) {

                    try {
                        String path = in.readUTF();
                        File file = new File(path);
                        long length = file.length();
                        out.writeLong(length);
                        FileInputStream fileBytes = new FileInputStream(file);
                        int read = 0;
                        byte[] buffer = new byte[256];
                        while ((read = fileBytes.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        fileBytes.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (command.equals("delete")) {
                    String path = in.readUTF();
                    Files.delete(Paths.get(path));
                }

                if (command.equals("rename")) {
                    String path = in.readUTF();
                    File file = new File(path);
                    String newPath = file.getParent() + "/" + in.readUTF();
                    File newFile = new File(newPath);
                    file.renameTo(newFile);
                }

                if (command.equals("make")) {
                    String path = in.readUTF();
                    File file = new File(path);
                    file.mkdir();
                }

            }

        } catch (SocketException socketException) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            System.out.println("Something happened in class ClientHandler. " +
                    "Something happened wrong with the threads (port " + socket.getPort() + ").");
        }
    }

    public List<FileInfo> makeFileInfo(Path path) {
        try {
            return Files.list(path).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace(); //TODO отрабоать в клиенте
            List<FileInfo> list = new ArrayList<>();
            list.add(new FileInfo("NA", FileInfo.FileType.FILE, 0L, LocalDateTime.MIN));
            return list;
        }
    }

    private void doAuth(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        while (true) {
            String message = in.readUTF();

            if (message.startsWith("auth")) {
                String[] msg = message.split(" ");
                ClientBase bd = new ClientBase();
                UserInfo possibleClient = bd.getRecord(msg[1]);
                if (possibleClient != null) {
                    if (possibleClient.getLogin().equals(msg[1]) && possibleClient.getPassword().equals(msg[2])) {
                        user = possibleClient;
                        System.out.println(String.format("User (port %s, log %s) logged-in.", socket.getPort(), msg[1]));
                        out.writeUTF("OK");
                        out.flush();
                        break;
                    } else if (possibleClient.getLogin().equals(msg[1])) {
                        out.writeUTF("No pass");
                        out.flush();
                    }
                } else {
                    out.writeUTF("No log");
                    out.flush();
                }
            }
            if (message.startsWith("singIn")) {
                String[] msg = message.split(" ");
                ClientBase bd = new ClientBase();
                UserInfo possibleClient = bd.getRecord(msg[1]);
                if (possibleClient != null) {
                    if (possibleClient.getLogin().equals(msg[1]) && possibleClient.getPassword().equals(msg[2])) {
                        out.writeUTF("occupied");
                        out.flush();
                    }
                } else {
                    Boolean isUserAdded = new ClientBase().addUser(msg[1], msg[2]);
                    if (isUserAdded) {
                        System.out.println("New user is added.");
                        user = new UserInfo(0, msg[1], msg[2]);
                        System.out.println(String.format("User (port %s, log %s) logged-in.", socket.getPort(), msg[1]));
                        out.writeUTF("OK");
                        out.flush();
                        File rootNewDir = new File("server/" + msg[1]);
                        rootNewDir.mkdirs();
                        break;
                    } else {
                        out.writeUTF("repeat");
                        out.flush();
                    }
                }
            }
        }
    }
}