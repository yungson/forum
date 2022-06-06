package org.example.forum.entity;

import java.util.List;

public class SearchResult {

    private List<DiscussPost> list;
    private long totalHits;
    public SearchResult(List<DiscussPost> list, long totalHits){
        this.list = list;
        this.totalHits = totalHits;
    }

    public List<DiscussPost> getList() {
        return list;
    }

    public void setList(List<DiscussPost> list) {
        this.list = list;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }
}
