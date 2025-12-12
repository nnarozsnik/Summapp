package hu.narnik.myapplication;

public class Note {
    private String id;
    private String title;
    private String content;
    private long createdAt;
    private String author;


    public Note() {}

    public Note(String id, String title, String content, long createdAt, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.author = author;

    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() { return createdAt; }

    public String getAuthor() { return author; }
}