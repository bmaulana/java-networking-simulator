/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

import java.util.ArrayList;


/**
 * 
 * %%%%%%%%%%%%%%%% YOU NEED TO IMPLEMENT THIS %%%%%%%%%%%%%%%%%%
 * 
 * Represents a network card that can be attached to a particular wire.
 * 
 * It has only two key responsibilities:
 * i) Allow the sending of data frames consisting of arrays of bytes using send() method.
 * ii) If a data frame listener is registered during construction, then any data frames
 *     received across the wire should be sent to this listener.
 *
 * @author K. Bryson
 */
public class NetworkCard extends Thread {
    
	// Wire pair that the network card is attached to.
    public TwistedWirePair wire;

    // Unique device name given to the network card.
    public String deviceName;
    
    // A 'data frame listener' to call if a data frame is received.
    public FrameListener listener;

    
    // Default values for high, low and mid- voltages on the wire.
    public double HIGH_VOLTAGE = 2.5;
    public double LOW_VOLTAGE = -2.5;
    
    // Default value for a signal pulse width that should be used in milliseconds.
    public int PULSE_WIDTH = 200;
    
    // Default value for maximum payload size in bytes.
    public int MAX_PAYLOAD_SIZE = 1500;

    
    /**
     * NetworkCard constructor.
     * @param deviceName This provides the name of this device, i.e. "Network Card A".
     * @param wire       This is the shared wire that this network card is connected to.
     * @param listener   A data frame listener that should be informed when data frames are received.
     *                   (May be set to 'null' if network card should not respond to data frames.)
     */
    public NetworkCard(String deviceName, TwistedWirePair wire, FrameListener listener) {
    	
    	this.deviceName = deviceName;
    	this.wire = wire;
    	this.listener = listener;
    	
    }

    /**
     * Tell the network card to send this data frame across the wire.
     * NOTE - THIS METHOD ONLY RETURNS ONCE IT HAS SEND THE DATA FRAME.
     * 
     * @param frame  Data frame to send across the network.
     */
    public void send(DataFrame frame) throws InterruptedException {
    	
		if (frame != null) {
			
			System.out.println("WAITING TO RECEIVE DATA FRAMES");
			
			// Low voltage signal to get ready ...
			wire.setVoltage(deviceName, LOW_VOLTAGE);
            //System.out.println("Set Low voltage signal to get ready");
			sleep(PULSE_WIDTH*4);
			
			byte[] payload = frame.getPayload();
			
			// Send bytes in asynchronous style with 0.2 seconds gaps between them.
			for (int i = 0; i < payload.length; i++) {
				
	    		// Byte stuff if required.
	    		if (payload[i] == 0x7E || payload[i] == 0x7D)
	    			sendByte((byte)0x7D);

	    		sendByte(payload[i]);
                //System.out.println("Sent byte: " + payload[i]);
			}
			
			// Append a 0x7E to terminate frame.
    		sendByte((byte)0x7E);
		}
    	
    }

    
	public void sendByte(byte value) throws InterruptedException {
        //System.out.println("Sending byte: " + value);

		// Low voltage signal ...
		wire.setVoltage(deviceName, LOW_VOLTAGE);
        //System.out.println("Set Low voltage signal");
		sleep(PULSE_WIDTH*4);

		// Set initial pulse for asynchronous transmission.
		wire.setVoltage(deviceName, HIGH_VOLTAGE);
        //System.out.println("Send Initial pulse");
		sleep(PULSE_WIDTH);
		
		// Go through bits in the value (big-endian bits first) and send pulses.
		
        for (int bit = 0; bit < 8; bit++) {
            if ((value & 0x80) == 0x80) {
                wire.setVoltage(deviceName, HIGH_VOLTAGE);
                //System.out.println("Sent High");
            } else {
                wire.setVoltage(deviceName, LOW_VOLTAGE);
                //System.out.println("Sent Low");
            }
            
            // Shift value.
            value <<= 1;  

            sleep(PULSE_WIDTH);
        }
	}

	/*
	 * If the listener is not null, the run method should "listen" to the wire,
	 * receive and decode any "data frames" that are transmitted,
	 * and inform the listener of any data frames received.
	 */
	public void run() {

		// YOU NEED TO IMPLEMENT THIS METHOD.
        ArrayList<Byte> bytes = new ArrayList<>();
        StringBuilder bits = new StringBuilder();
        int bit = 0;
        boolean receivingByte = false;

        if(listener != null) {
            while(true) {
                int volt; //low = 0, high = 1

                if(wire.getVoltage(deviceName) > 0){
                    //System.out.println("Received High");
                    volt = 1;
                } else {
                    //System.out.println("Received Low");
                    volt = 0;
                }

                if(receivingByte) {
                    bits.append(volt);
                    bit++;

                    if(bit == 8) {
                        receivingByte = false;

                        //System.out.println("Received Byte: "+ bits.toString());
                        int decimal = Integer.parseInt(bits.toString(),2);
                        Byte aByte = new Byte(Integer.toString(decimal));
                        bytes.add(aByte);
                        System.out.println("RECEIVED BYTE: " + Integer.toHexString(decimal));

                        if(aByte.intValue() == 126) { //received sentinel
                            //System.out.println("Message end");
                            break;
                        }
                    }
                } else if(volt == 1){
                    receivingByte = true;
                    bit = 0;
                    bits = new StringBuilder();
                    System.out.println("WAITING ...");
                }

                try {
                    Thread.sleep(PULSE_WIDTH);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size()-1; i++) {
                byteArray[i] = bytes.get(i);
            }
            listener.receive(new DataFrame(byteArray));
        }
	}
	
	
}
