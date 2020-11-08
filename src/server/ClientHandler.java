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
//            out.writeUTF("authOK");
//            out.flush();
            File rootDir = new File("server/"+user.getLogin()+"/");
            out.writeUTF(rootDir.getPath());
//            System.out.println("rootDir отправлен");
            //out.writeObject(makeFileInfo(Paths.get(rootDir.getPath())));
            out.flush();

            while (true) {
//                System.out.println("в блоке while");
                String command = in.readUTF();
//                System.out.println("command: " + command);

                // получение информации для обновления таблицы
                if (command.startsWith("info")) {
//                    System.out.println("info command");
                    String path = command.replace("info", "");
                    File file = new File(path);
//                    System.out.println("path: " + path);
                    if (file.exists()) {
                        out.writeObject(makeFileInfo(Paths.get(path)));
                        out.flush();
//                        System.out.println("makeFileInfo(Paths.get(path) made");
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
//                        System.out.println("out.writeUTF: File is copied.");
                    } catch (Exception e) {
                        out.writeUTF("WRONG");
                    }
                }
                // копирование с сервера на клиент
                if (command.equals("download")) {
//                    System.out.println("in block IF download");
                    try {
                        String path = in.readUTF();
//                        System.out.println("path: " + path);
                        File file = new File(path);
                        long length = file.length();
                        out.writeLong(length);
//                        System.out.println("send size: " + length);
                        FileInputStream fileBytes = new FileInputStream(file);
                        int read = 0;
                        byte[] buffer = new byte[256];
                        while ((read = fileBytes.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        fileBytes.close();
//                        System.out.println(" out.flush(): OK");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (command.equals("delete")) {
//                    System.out.println("in block IF DELETE");
                    String path = in.readUTF();
//                    System.out.println("path: " + path);
//                    File file = new File(path);
                    Files.delete(Paths.get(path));
                }

                if (command.equals("Hello")) { //todo заменить на авторизацию
                    System.out.println("На прием работаю!");
                    out.writeUTF("Hi!");
                }
//                System.out.println(command);
            }

        } catch (SocketException socketException) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            e.printStackTrace();
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
                System.out.println("log: "+msg[1]+".");
                System.out.println("log: "+msg[2]+".");
                AuthService.Record possibleUser = authService.findRecord(msg[1], msg[2]);
                if (possibleUser != null) {
                    user = possibleUser;
                    System.out.println("User logged-in.");
                    break;
                }
            }
        }

    }
}