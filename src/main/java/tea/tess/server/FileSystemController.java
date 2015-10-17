package tea.tess.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import tea.tess.server.exceptions.UnknownCommandException;
import tea.tess.server.exceptions.UnsupportedFileTypeException;
import tea.tess.server.exceptions.WrongCommandFormatException;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by arseniy on 12.10.15.
 */
public class FileSystemController {
    private PrintWriter out;
    private Path rootDirectory;
    private Path currentDirectory;
    private static String changeDirCommand, createDirCommand, displayDirCommand, createFileCommand, showFileCommand,
            copyCommand, removeCommand, moveCommand, uploadFileCommand, downloadFileCommand;
    public static final String FILE_HELP = "Type 'help' to get help.";

    public FileSystemController(String changeDirCommand, String createDirCommand, String displayDirCommand, String createFileCommand, String showFileCommand,
                                String copyCommand, String removeCommand, String moveCommand, String uploadFileCommand, String downloadFileCommand) {
        FileSystemController.changeDirCommand = changeDirCommand;
        FileSystemController.createDirCommand = createDirCommand;
        FileSystemController.displayDirCommand = displayDirCommand;
        FileSystemController.createFileCommand = createFileCommand;
        FileSystemController.showFileCommand = showFileCommand;
        FileSystemController.copyCommand = copyCommand;
        FileSystemController.removeCommand = removeCommand;
        FileSystemController.moveCommand = moveCommand;
        FileSystemController.uploadFileCommand = uploadFileCommand;
        FileSystemController.downloadFileCommand = downloadFileCommand;
    }

    public FileSystemController(PrintWriter pw, Path current) {
        out = pw;
        currentDirectory = current;
        rootDirectory = current;
    }

    public void sendResponce(String message) {
        out.println(message);
    }

    public void sendError(String message) {
        out.println("ERROR :: " + message);
    }

    public void recognize(String message) throws UnknownCommandException, IOException {
        int to = message.indexOf(" ");
        String command, params = "";
        if (to == -1)
            to = message.length();
        else
            params = message.substring(to + 1);
        command = message.substring(0, to);
        String[] p = params.split(" ");
        String wrongParamsCount = "Unknown count of parameters: ";

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {"config.xml"});
        FileSystemController controller = (FileSystemController) applicationContext.getBean("FScontroller");
        try {
            if (command.equals(controller.getChangeDirCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                cd(params);
            } else if (command.equals(controller.getCreateDirCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                mkdir(params);
            } else if (command.equals(controller.getDisplayDirCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                ls(params);
            } else if (command.equals(controller.getCreateFileCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                touch(params);
            } else if (command.equals(controller.getShowFileCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                cat(params);
            } else if (command.equals(controller.getCopyCommand())) {
                if (p.length != 2)
                    throw new WrongCommandFormatException("Unknown count of parameters: " + p.length);
                cp(p[0], p[1]);
                sendResponce("Copying completed successfully");
            } else if (command.equals(controller.getRemoveCommand())) {
                if (p.length != 1)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                rm(params);
                sendResponce("Deleting completed successfully");
            } else if (command.equals(controller.getMoveCommand())) {
                if (p.length != 2)
                    throw new WrongCommandFormatException(wrongParamsCount + p.length);
                mv(p[0], p[1]);
                sendResponce("Moving completed successfully");
            } else if (command.equals("help")) {
                help();
            }
// else if (command.equals(controller.getUploadFileCommand())) {
// push(params);
// } else if (command.equals(controller.getDownloadFileCommand())) {
// get(params);
// }
            else
                throw new UnknownCommandException("No such command found: " + command);
        } catch (WrongCommandFormatException e) {
             sendError(e.getMessage());
        } catch (SecurityException e) {
             sendError(e.getMessage());
        } catch (UnsupportedFileTypeException e) {
            sendError(e.getMessage());
        } catch (IOException e) {
            sendError(e.getMessage());
        }
    }

    private boolean isOutOfRoot(Path file) {
        return !file.normalize().toString().startsWith(rootDirectory.toString());
    }

    private String getUsersPath (Path path) throws SecurityException {
        if (!isOutOfRoot(path)) {
            return path.toString().substring(rootDirectory.getParent().toString().length());
        } else
            throw new SecurityException("Out of root!");
    }

    /**
     * Function cd - changes current directory.
     * Usage:
     * cd path
     */

    private void cd(String path) throws WrongCommandFormatException, IOException {
        Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (Files.exists(file)) {
            currentDirectory = file;
            String usersPath = getUsersPath(currentDirectory);
            if (usersPath != null)
                sendResponce(usersPath);
            else
                throw new NullPointerException();
        }
        else
            throw new FileNotFoundException("No such directory: " + file);
    }

    /**
     * Function mkdir - creates a new directory.
     * Usage:
     * mkdir path
     */

    private void mkdir(String path) throws IOException {
        Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (!Files.exists(file)) {
            Files.createDirectory(file);
            String usersPath = getUsersPath(file);
            if (usersPath != null)
                sendResponce("Directory " + usersPath + " created successfully");
            else
                throw new NullPointerException();
        }
        else
            throw new FileAlreadyExistsException("Directory already exists: " + path);
    }

    /**
     * Function ls - displays the contents of a directory.
     * Usage:
     * ls [path]
     */

    private void ls(String path) throws IOException {
        Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (Files.exists(file))
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(file);
                try {
                    StringBuilder builder = new StringBuilder();
                    for (Path p : stream) {
                        builder.append(p.getFileName() + "\n");
                    }
                    sendResponce(builder.toString());
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                sendResponce("Error while reading directory. Try again later.");
            }
        else
            throw new FileNotFoundException("No such directory: " + file);
    }

    /**
     * Function touch - creates a new file.
     * Usage:
     * touch path
     */

    private void touch(String path) throws IOException {
        Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (!Files.exists(file)) {
            Files.createFile(file);
            String usersPath = getUsersPath(file);
            if (usersPath != null)
                sendResponce("File " + usersPath + " created successfully");
            else
                throw new NullPointerException();
        }
        else
            throw new FileAlreadyExistsException("File already exists: " + path);
    }

    /**
     * Function cat - displays content of text file or image to user.
     * Usage:
     * cat path
     */

    private void cat(String path) throws IOException, UnsupportedFileTypeException {
        Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (Files.exists(file)) {
            if (file.getFileName().toString().endsWith(".txt")) {
                RandomAccessFile aFile = new RandomAccessFile(file.toString(), "r");
                FileChannel inChannel = aFile.getChannel();
                MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
                buffer.load();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < buffer.limit(); i++) {
                    result.append((char) buffer.get());
                }
                buffer.clear();
                inChannel.close();
                aFile.close();
                sendResponce(result.toString());
            } else
                throw new UnsupportedFileTypeException("Unsupported file extension: " + file.getFileName().toString());
        }
        else
            throw new FileNotFoundException("No such directory: " + file);
    }

    /**
     * Function cp - copy file or directory "path1" to "path2".
     * Usage:
     * cp path1 path2
     */

    private void cp(String path1, String path2) throws IOException {
        final Path file1 = Paths.get(currentDirectory + File.separator + path1).normalize();
        final Path file2 = Paths.get(currentDirectory + File.separator + path2).normalize();
        if (isOutOfRoot(file1) || isOutOfRoot(file2))
            throw new SecurityException("Out of root!");
        if (!Files.exists(file1)) {
            throw new NoSuchFileException("No such file or directory exists: " + path1);
        } else {
            if (!Files.isDirectory(file1)) {
                if (Files.exists(file2) && !Files.isDirectory(file2))
                    throw new FileAlreadyExistsException("File already exists: " + path2);
                Files.copy(file1, file2);
            } else
                try {
                    if (!Files.exists(file2))
                        Files.createDirectory(file2);
                    Files.walkFileTree(file1, new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attribs) throws IOException {
                            String subDir = path.toString().substring(file1.toString().length());
                            try {
                                Files.createDirectories(Paths.get(file2 + File.separator + subDir.substring(0, subDir.lastIndexOf(File.separator))));
                                Files.copy(path, Paths.get(file2 + File.separator + subDir));
                            } catch (IOException e) {} // For merging
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
        }
    }

    /**
     * Function rm - removes file or directory.
     * Usage:
     * rm path
     */

    private void rm(String path) throws IOException { //InputStream file, String name) {
        final Path file = Paths.get(currentDirectory + File.separator + path).normalize();
        if (isOutOfRoot(file))
            throw new SecurityException("Out of root!");
        if (!Files.exists(file)) {
            throw new NoSuchFileException("No such file exists: " + file);
        } else {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Function mv - move file or directory (recursively) from one path to another.
     * Usage:
     * mv path1 path2
     */

    private void mv(String path1, String path2) throws IOException { //InputStream file, String name) {
        cp(path1, path2);
        rm(path1);
    }

    /**
     * Function help - tells user about other functions.
     * Usage:
     * help
     */

    private void help() throws IOException { //InputStream file, String name) {
        sendResponce(HELP_MESSAGE);
    }

    private static final String HELP_MESSAGE = "cd path -- changes current directory.\n" +
            "mkdir path -- creates a new directory.\n" +
            "ls [path] -- displays the contents of a directory.\n" +
            "touch path -- creates a new file.\n" +
            "cat path -- displays content of text file to user.\n" +
            "cp path1 path2 -- copy file or directory \"path1\" to \"path2\".\n" +
            "rm path -- removes file or directory.\n" +
            "mv path1 path2 -- moves file or directory (recursively) from one path to another.";

    public String getChangeDirCommand() {
        return changeDirCommand;
    }

    public void setChangeDirCommand(String changeDirCommand) {
        FileSystemController.changeDirCommand = changeDirCommand;
    }

    public String getCreateDirCommand() {
        return createDirCommand;
    }

    public void setCreateDirCommand(String createDirCommand) {
        FileSystemController.createDirCommand = createDirCommand;
    }

    public String getDisplayDirCommand() {
        return displayDirCommand;
    }

    public void setDisplayDirCommand(String displayDirCommand) {
        FileSystemController.displayDirCommand = displayDirCommand;
    }

    public String getCreateFileCommand() {
        return createFileCommand;
    }

    public void setCreateFileCommand(String createFileCommand) {
        FileSystemController.createFileCommand = createFileCommand;
    }

    public String getShowFileCommand() {
        return showFileCommand;
    }

    public void setShowFileCommand(String showFileCommand) {
        FileSystemController.showFileCommand = showFileCommand;
    }

    public String getUploadFileCommand() {
        return uploadFileCommand;
    }

    public void setUploadFileCommand(String uploadFileCommand) {
        FileSystemController.uploadFileCommand = uploadFileCommand;
    }

    public String getDownloadFileCommand() {
        return downloadFileCommand;
    }

    public void setDownloadFileCommand(String downloadFileCommand) {
        FileSystemController.downloadFileCommand = downloadFileCommand;
    }

    public String getCopyCommand() {
        return copyCommand;
    }

    public void setCopyCommand(String copyCommand) {
        FileSystemController.copyCommand = copyCommand;
    }

    public String getRemoveCommand() {
        return removeCommand;
    }

    public void setRemoveCommand(String removeCommand) {
        FileSystemController.removeCommand = removeCommand;
    }

    public String getMoveCommand() {
        return moveCommand;
    }

    public void setMoveCommand(String moveCommand) {
        FileSystemController.moveCommand = moveCommand;
    }
}