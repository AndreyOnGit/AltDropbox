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
}
