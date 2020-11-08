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


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }


    @Override
    public void run() {

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            //Отправка начальной информации
            File rootDir = new File("server/");
            out.writeUTF(rootDir.getPath());
            System.out.println("rootDir отправлен");
            //out.writeObject(makeFileInfo(Paths.get(rootDir.getPath())));
            out.flush();

            while (true) {
                System.out.println("в блоке while");
                String command = in.readUTF();
                System.out.println("command: " + command);

                if (command.startsWith("info")) {
                    System.out.println("info command");
                    String path = command.replace("info", "");
                    File file = new File(path);

                    System.out.println("path: " + path);
                    if (file.exists()) {
                        out.writeObject(makeFileInfo(Paths.get(path)));
                        out.flush();
                        System.out.println("makeFileInfo(Paths.get(path) made");
                    } else System.out.println("Attention. There ia failure to find the desired file or folder.");
                }

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
//                        out.writeUTF("OK");
//                        System.out.println("Ok");
                    } catch (Exception e) {
                        out.writeUTF("WRONG");
                    }
                }
                if (command.equals("download")) {
                    // TODO: 27.10.2020
                    try {
                        File file = new File("server/" + in.readUTF());
                        long length = file.length();
                        out.writeLong(length);
                        FileInputStream fileBytes = new FileInputStream(file);
                        int read = 0;
                        byte[] buffer = new byte[256];
                        while ((read = fileBytes.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        String status = in.readUTF();
                        System.out.println(status);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (command.equals("exit")) {
                    System.out.println("Client disconnected correctly");
                    // out.writeUTF("OK");
                    break;
                }
                if (command.equals("Hello")) {
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

//    public void sendList(String path) {
//        List<FileInfo> list = makeFileInfo(Paths.get(path));
//        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
//            out.writeObject(list);
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}