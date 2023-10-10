package tp1.impl.dropbox.args;

public class Upload{
    final String path;
    final String mode;
    final boolean autorename;
    final boolean mute;
    final boolean strict_conflict;

    public Upload(String path, String mode) {
        this.path = path;
        this.mode = mode;
        this.autorename = false;
        this.mute = false;
        this.strict_conflict = false;
    }
}