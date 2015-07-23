package be.monfils.x10clone.dcpu;

import be.monfils.x10clone.rendering.X10clone;

import java.util.LinkedList;

/**
 * Created by nathan on 22/07/15.
 */
public class DCPUManager {
	protected DCPU dcpu;
	protected LinkedList<LEM1802> lems = new LinkedList<>();
	protected LinkedList<GenericClock> clocks = new LinkedList<>();
	protected LinkedList<GenericKeyboard> keyboards = new LinkedList<>();
	protected char[] initialRam;

	public DCPUManager(DCPU dcpu, char[] ram, int nLems, int nClocks, int nKeyboards) {
		this.dcpu = dcpu;
		for(int i = 0; i < nLems; ++i) {
			lems.add(X10clone.hardwareTracker.requestLem());
			lems.getLast().connectTo(dcpu);
			lems.getLast().powerOn();
		}

		for(int i = 0; i < nClocks; ++i) {
			clocks.add(X10clone.hardwareTracker.requestClock());
			clocks.getLast().connectTo(dcpu);
			clocks.getLast().powerOn();
		}

		for(int i = 0; i < nLems; ++i) {
			keyboards.add(X10clone.hardwareTracker.requestKeyboard());
			keyboards.getLast().connectTo(dcpu);
			keyboards.getLast().powerOn();
		}

		initialRam = ram;
		dcpu.setRam(ram);
	}

	public void startDCPU() {
		dcpu.start();
		System.out.println("Started DCPU");
	}

	public void stopDCPU() {
		dcpu.setStopped();
	}

	public void tick() {
		for(LEM1802 lem : lems)
			lem.tick60hz();
		for(GenericClock clock : clocks)
			clock.tick60hz();
		for(GenericKeyboard keyboard : keyboards)
			keyboard.tick60hz();
	}

	public DCPU getDcpu() {
		return dcpu;
	}

	public LinkedList<LEM1802> getLems() {
		return lems;
	}

	public LinkedList<GenericClock> getClocks() {
		return clocks;
	}

	public LinkedList<GenericKeyboard> getKeyboards() {
		return keyboards;
	}

	/*
	 * DEBUG
	 */
	public String dump() {
		String dump = "PC : " + (int) dcpu.pc + "\nSP : " + (int) dcpu.sp + "\nEX : " + (int) dcpu.ex + "\nIA " + (int) dcpu.ia + "\n" +
				"A : " + (int) dcpu.registers[0] + "\nB : " + (int) dcpu.registers[1] + "\nC : " + (int) dcpu.registers[2] + "\nX : " + (int) dcpu.registers[3] + "\nY : " + (int) dcpu.registers[4] + "\nZ : " + (int) dcpu.registers[5] + "\nI : " + (int) dcpu.registers[6] + ", J : " + (int) dcpu.registers[7] + "\n" +
				"cycles : " + (int) dcpu.cycles;
		return dump;
	}
}
