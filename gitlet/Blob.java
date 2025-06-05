package gitlet;

import java.io.File;

import static gitlet.Utils.*;

public class Blob implements Dumpable {
    private final String objectType = "blob";
    private final byte[] fileContent;

    public Blob(File file) {
        fileContent = readContents(file);
    }

    public Blob(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public String getObjectType() {
        return objectType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    @Override
    public void dump(){}
}
