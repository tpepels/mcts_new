package pentalath.game;


public class Field {
    public int occupant = Board.FREE;
    public int position, freedom, numNeighbours = 0;
    public Field[] neighbours = new Field[6];

    public Field(int position) {
        this.position = position;
    }
}
