#Import neccessary libraries
import smbus
import json
import time 
import paho.mqtt.client as mqtt
import RPi.GPIO as GPIO

#Read the TVOC measurement
def readtvoc():
	qualitylist=bus.read_i2c_block_data(0x5b,0x02,4)
    #The 2nd and 3rd byte contain information on the current TVOC in the room
    tvoclist=qualitylist[2:]
	tvoc=int.from_bytes(tvoclist,byteorder='big')
	time.sleep(0.2)
	return tvoc;

#Receiving messages from the topic:"IC.embedded/SMARTasses/start" and
#set the global variable session
def on_message(client,userdata,message):
	session=message.payload.decode("utf-8")
	global mySession
	mySession = session

#Read the air flow sensor
def readflow():
	flowlist=bus.read_i2c_block_data(0x48,0x00,2)
	flow=int.from_bytes(flowlist,byteorder='big')
	time.sleep(0.2)
	return flow;


def attemptconnect(client):
    try:
        client.connect("test.mosquitto.org",port=8884)
        #only sets connected_flag to true if no exception is thrown
        client.connected_flag=True
    except:
        print("connection failed")



#Send data function. This function is triggered when session has started.
#It calls the measurement functions and performs the necessary calculation to
#determine if the whole dose in the space has been taken or if the user pressed the inhaler
#button too soon. Finally publishes the results back to the topic: "IC.embedded/SMARTasses/data"
def send_data():
	global chamberempty
	global newpress
	global breaths
	global possible_error
	global wait
    #Call measurement functions
    tvoc=readtvoc()
	flow=readflow()
    #Condition to measure new breaths
    if (flow<100):
		if (not wait):
			breaths = breaths + 1
			wait = True
	else:  
		wait=False

    #Condition to determine if the whole dose in the space has
    #been taken or if the user pressed the inhaler
    #button too soon.
	if(tvoc>25000):
        #Check if the chamber was empty
		if(chamberempty):
			chamberempty=False
			breaths=0
			newpress=True
			GPIO.output(16,GPIO.HIGH)
			client.publish("IC.embedded/SMARTasses/press","1")
			possible_error=False
			wait = False
        #If the user has began taking the medicine and TVOC has previously dropped below 25000 publish error
		elif(possible_error):
			payload={"breath": breaths,"error":"1"}
			print(payload)
			GPIO.output(16,GPIO.LOW)
			time.sleep(0.5)
			GPIO.output(16,GPIO.HIGH)
			client.publish("IC.embedded/SMARTasses/data",json.dumps(payload))
			possible_error = False
    #Set possible error flag to true as the user has begun breathing in the new dose
    elif(tvoc>4000):
		if(not chamberempty):
			possible_error=True
	else:
        #check if the user has initially pressed the button for their first dose and hasnt just started breathing in if true then publish measurements for
		if(newpress):
			chamberempty=True
			GPIO.output(16,GPIO.LOW)
			payload={"breath": breaths,"error":"0"}
			print (payload)
			newpress=False
			client.publish("IC.embedded/SMARTasses/data",json.dumps(payload))

#Initialiaze a bus object by calling the smbus.SMBus() constructor
bus=smbus.SMBus(1)
#Initialiaze a client object by calling the mqtt.Client() constructor
client = mqtt.Client()
#Set up the client to successfully publish / subscribe using encryption to the topic:
#IC.embedded/SMARTasses/start
client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
#sets function previously defined function "on_message" to be a member function of the client class
client.on_message=on_message

#Stays in this loop untill a connection has been established
while (not client.connected_flag): #wait in loop
    print("Attempting to connect to broker")
    attemptconnect(client)
        time.sleep(1)

#Once connected subscribes to the start topic
client.subscribe("IC.embedded/SMARTasses/start")






#Initialize global variables
chamberempty=True #required to initially specify that the chamber is empty
newpress=False #required to specify that the child has pressed the button for the first time in that session
breaths=0 #initialized to zero before first press
mySession="0" #initialized to zero such that the script waits for the start message
possible_error=False #required to wait for TVOC to go below thress hold of new dose before checking if they have pressed again (otherwise an error would be published on loop until they start breathing)
wait = False #required to wait for Airflow to return to normal before checking for new breath (a breath will normaly force airflow down for more then one measurement

#Initialize GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)
GPIO.setup(16,GPIO.OUT)

#Main loop of this script
while(True):
	client.loop()
    #If session then trigger send_Data() function
	if(mySession=="1"):
		send_data()
	else:
        #If not session reset global variables
		chamberempty=True
		newpress=False
		breaths=0
		mySession="0"
		possible_error=False
		wait = False
		GPIO.output(16,GPIO.LOW)


