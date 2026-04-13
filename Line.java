public class Line {

    // start, end => 連線的起點和終點
    // type => 連線的類型 (association, aggregation, composition)
    private Port start; 
    private Port end;   
    private String type; 

    // constructor
    public Line(Port start, Port end, String type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    // getter
    public Port getStart() { return start; }
    public Port getEnd() { return end; }
    public String getType() { return type; }

    // setter    
    public void setStart(Port start) { this.start = start; }
    public void setEnd(Port end) { this.end = end; }
    public void setType(String type) { this.type = type; }
}