import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class PathArray {
    public static ArrayList<Path> arrayList = new ArrayList<>();

    public static ArrayList<Path> makePathArray(Path path){
        arrayList.add(path);
        return arrayList;
    }

    public void clear(){
        arrayList.clear();
    }

    public static boolean exists (Path path){
        File file = new File(String.valueOf(path));
        if (file.exists()) return true;
        else return false;
    }
}
