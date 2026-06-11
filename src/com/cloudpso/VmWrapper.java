package com.cloudpso;

public class VmWrapper {
    public int vmId;
    public double mips;
    public boolean isFailed;

    public VmWrapper(int vmId, double mips) {
        this.vmId = vmId;
        this.mips = mips;
        this.isFailed = false;
    }
}