package server;

import client.FileInfo;

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
    private AuthService.Record user;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            doAuth(in);
            File rootDir = new File("server/" + user.getLogin() + "/");
            out.writeUTF(rootDir.getPath());
            out.flush();

            while (true) {
                String command = in.readUTF();

                // получение информации для обновления таблицы
                if (command.startsWith("info")) {
                    String path = command.replace("info", "");
                    File file = new File(path);
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
//                    System.out.println("path: " + path);
                    File file = new File(path);
                    String newPath = file.getParent() + "/" + in.readUTF();
//                    System.out.println("newPath: " + newPath);
                    File newFile = new File(newPath);
                    file.renameTo(newFile);
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

    private void doAuth(ObjectInputStream in) throws IOException {
        AuthService authService = new BasicAuthService();
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("auth")) {
                String[] msg = message.split(" ");
                AuthService.Record possibleUser = authService.findRecord(msg[1], msg[2]);
                if (possibleUser != null) {
                    user = possibleUser;
                    System.out.println(String.format("User (port %s, log %s) logged-in.", socket.getPort(), msg[1]));
                    break;
                }
            }
        }
    }
}