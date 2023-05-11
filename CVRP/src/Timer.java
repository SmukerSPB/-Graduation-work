public class Timer {
    private double startTime = 0.0;
    private  double stopTime = 0.0;
    private boolean running = false;

    public void Clear() {
        this.startTime = 0.0;
        this.stopTime = 0.0;
        this.running = false;
    }

    public void Start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public void Stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
    }

    public  double Result()
    {
        return (stopTime -  startTime)/1000;
    }
}
