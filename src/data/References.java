package data;

import java.util.ArrayList;

public class References {
    private String raw;
    private ArrayList<Integer> articleIDs = null;

    public References() {
    }

    public References(String raw) {
        this.raw = raw;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public ArrayList<Integer> getArticleIDs() {
        return articleIDs;
    }

    public void setArticleIDs(ArrayList<Integer> articleIDs) {
        this.articleIDs = articleIDs;
    }

    public void addArticleID(Integer id) {
        if (articleIDs != null) {
            articleIDs.add(id);
        }
    }
}
