#Import neccessary libraries
import paho.mqtt.client as mqtt
import smbus
import time

#Function to receive message for the broker from the topic: IC.embedded/SMARTasses/setup
def on_message(client,userdata,message):
	message.payload=message.payload.decode("utf-8")
    #If the message is 1 then start setup
	if(message.payload=="1"):
        #the following lines of code are used to set up the CCS811 sensor on address bus 0x5b
        emptylist=[]
        #writing an empty list to register #0xF4 switches the sensor from boot mode to application mode
		bus.write_i2c_block_data(0x5b,0xF4,emptylist)
        #the sleep commands in this process are given as per the data sheet to allow the address bus to settle before data is passed through them once more
        time.sleep(0.2)
        #sets the drive mode as instructed in the data-sheet to mode 1 which takes a measurement every second
        drivemode=[0b00010000]
		bus.write_i2c_block_data(0x5b,0x01,drivemode)
		time.sleep(0.2)
		currentmode=bus.read_i2c_block_data(0x5b,0x01,1)
        #The ADC does not need to be initialised as its ready to measure from boot and we are using its default parameters
        #If correct setup then publish 0 back to the topic
		if(currentmode==[16]):
			client.publish("IC.embedded/SMARTasses/setup","0")
		else:
			client.publish("IC.embedded/SMARTasses/setup","Error in initialisation check sensor")

def attemptconnect(client):
    try:
        client.connect("test.mosquitto.org",port=8884)
        #only sets connected_flag to true if no exception is thrown
        client.connected_flag=True
    except:
        print("connection failed")

#Initialiaze a bus object by calling the smbus.SMBus() constructor
bus = smbus.SMBus(1)
#Initialiaze a client object by calling the mqtt.Client() constructor
client=mqtt.Client()
#Set up the client to successfully publish / subscribe using encryption to the topic:
#IC.embedded/SMARTasses/setup
client.tls_set(ca_certs="mosquitto.org.crt",certfile="client.crt",keyfile="client.key")
#sets function previously defined function "on_message" to be a member function of the client class
client.on_message=on_message

#Stays in this loop untill a connection has been established
while (not client.connected_flag): #wait in loop
    print("Attempting to connect to broker")
    attemptconnect(client)
        time.sleep(1)

#Once connected subscribes to the setup topic
client.subscribe("IC.embedded/SMARTasses/setup")

#Main loop of this script
while(True):
	client.loop()
	time.sleep(2)
