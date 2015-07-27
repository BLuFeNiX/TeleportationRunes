package net.blufenix.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class UIDGrabber extends ObjectInputStream {

	public UIDGrabber(InputStream in) throws IOException {
		super(in);
	}

	public long getSerialVersionUID() throws ClassNotFoundException, IOException {
		return this.readClassDescriptor().getSerialVersionUID();
	}
}
