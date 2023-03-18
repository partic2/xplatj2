package pursuer.patchedmsgpack.compat;

public class Instant{
    protected int nano;
    protected long second;
    public Instant(long second,int nano){
        this.second=second;
        this.nano=nano;
    }
    public int getNano(){return nano;}
    public long getEpochSecond(){return second;}
    public long toEpochMilli(){return second*1000+nano/1000000;}
    public static Instant ofEpochMilli(long a){return new Instant(a/1000,(int)(a%1000*1000000));}
    public static Instant ofEpochSecond(long epochSecond, int nanoAdjustment){return new Instant(epochSecond,nanoAdjustment);}
    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment){return new Instant(epochSecond,(int)nanoAdjustment);}
    public static Instant ofEpochSecond(long sec){return new Instant(sec,0);}
}