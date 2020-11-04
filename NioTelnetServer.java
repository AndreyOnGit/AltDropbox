import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class NioTelnetServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final String rootPath = "server"; // имя папки
    private String currentPath;
    private final String commands = "input following:\n\r" +
            "ls to show file list\n\r" +
            "cd name_folder to go to folder with name \"name_folder\"\n\r" +
            "touch name_file to create .txt file with name \"name_file\"\n\r" +
            "mkdir name_folder to create new folder with name \"name_folder\"\n\r" +
            "rm name_file to delete file with name \"name_file\"\n\r" +
            "copy name_file new_directory to copy file from current directory \"name_file\" to new directory\n\r" +
            "cat file to show into console the contents of the file\n\r";

    SocketChannel channel;

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started!");
        currentPath = rootPath;
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();
        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");  //символ возврата каретки
        System.out.println(command);
        if (command.equals("--help")) {
            channel.write(ByteBuffer.wrap(commands.getBytes()));
        }
        if (command.equals("ls")) {
            channel.write(ByteBuffer.wrap((getFilesList(currentPath) + "\n\r").getBytes()));
        }
        if (command.startsWith("cd ")) {
            goToFolder(command);
        }
        if (command.startsWith("touch ")) {
            makeFile(command);
        }
        if (command.startsWith("mkdir ")) {
            makeFolder(command);
        }
        if (command.startsWith("rm ")) {
            removeFile(command);
        }
        if (command.startsWith("copy ")) {
            copyFile(command);
        }
        if (command.startsWith("cat ")) {
            showFile(command);
        }

    }

    private void showFile(String command) throws IOException {
        String[] cmd = command.split(" ");
        File file = new File(currentPath + "/" + cmd[1]);
        if (file.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) channel.write(ByteBuffer.wrap((line + "\n\r").getBytes()));
            ;
        } else {
            channel.write(ByteBuffer.wrap(String.format("Error. File %s does not exist.\n\r", cmd[1]).getBytes()));
        }
    }

    private void copyFile(String command) throws IOException {
        /*только для реализации копирования из текущей директории в директорию внутри корневой папки*/
        String[] cmd = command.split(" ");
        File file = new File(currentPath + "/" + cmd[1]);
        File dir = new File(rootPath + "/" + cmd[2]);
        File destFile = new File(dir, file.getName());
        if (file.exists()) {
            if (!dir.exists()) dir.mkdir(); //без раелизации создания поддиректории
            Files.copy(file.toPath(), destFile.toPath());
        } else {
            channel.write(ByteBuffer.wrap(String.format("Error. File %s does not exist.\n\r", cmd[1]).getBytes()));
        }
    }

    private void removeFile(String command) throws IOException {
        /*только для реализации удаления в текущей папке*/
        String[] cmd = command.split(" ");
        File file = new File(currentPath + "/" + cmd[1]);
        if (file.exists()) {
            file.delete();
            channel.write(ByteBuffer.wrap(String.format("File %s has been deleted.\n\r", cmd[1]).getBytes()));
        } else {
            channel.write(ByteBuffer.wrap(String.format("Error. File %s does not exist.\n\r", cmd[1]).getBytes()));
        }
    }

    private void makeFolder(String command) throws IOException {
        String[] cmd = command.split(" ");
        File file = new File(currentPath + "/" + cmd[1]);
        if (!file.exists()) {
            file.mkdir();
            channel.write(ByteBuffer.wrap(String.format("Folder %s has been created.\n\r", cmd[1]).getBytes()));
        } else {
            channel.write(ByteBuffer.wrap(String.format("Error. Folder %s already exists.\n\r", cmd[1]).getBytes()));
        }
    }

    private void makeFile(String command) throws IOException {
        String[] cmd = command.split(" ");
        File file = new File(currentPath + "/" + cmd[1] + ".txt");
        if (!file.exists()) {
            file.createNewFile();
            channel.write(ByteBuffer.wrap(String.format("File %s.txt has been created.\n\r", cmd[1]).getBytes()));
        } else {
            channel.write(ByteBuffer.wrap(String.format("Error. File %s.txt already exists.\n\r", cmd[1]).getBytes()));
        }
    }

    private void goToFolder(String command) throws IOException {
        String[] cmd = command.split(" "); //Ограничение на файлы с именем на разделение имени с пробелом
        currentPath = currentPath + "/" + cmd[1];
        channel.write(ByteBuffer.wrap(String.format("Current path is %s.\n\r", cmd[1]).getBytes()));
    }

    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel) key.channel())
                        .write(ByteBuffer.wrap(message.getBytes()));
            }
        }
    }

    private String getFilesList(String currentPath) {
        File fl = new File(currentPath);
        if (fl.list() != null)
            return String.join(" ", fl.list());
        else return "";
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "LOL");
        channel.write(ByteBuffer.wrap("Enter --help\n\r".getBytes()));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}