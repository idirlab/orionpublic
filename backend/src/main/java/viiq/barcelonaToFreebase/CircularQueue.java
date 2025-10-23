package viiq.barcelonaToFreebase;

import it.unimi.dsi.lang.MutableString;

import org.apache.log4j.Logger;

public class CircularQueue
{
	final Logger logger = Logger.getLogger(getClass());
	int queueSize = -1;
	String[] queueElement;
	int front = 0;
	int rear = 0;
	int frontIndex = 0;
	int rearIndex = 0;
	int numOfActiveElements = 0;
	
	public CircularQueue(int queueSize)
	{
		this.queueSize = queueSize;
		queueElement = new String[queueSize];
	}
	
	public void enqueue(String properties)
	{
		queueElement[front] = properties;
		frontIndex++;
		front = frontIndex % queueSize;
		numOfActiveElements++;
	}
	
	public void dequeue()
	{
		//String line = queueElement[rear];
		rearIndex++;
		rear = rearIndex % queueSize;
		numOfActiveElements--;
	}
	
	public MutableString seek(int numOfElements)
	{
		MutableString concatenatedProps = new MutableString();
		if(rear >= front)
		{
			// count from numOfElements from rear to end AND from 0 to front-1;
			for(int i=rear; i<queueSize; i++)
			{
				if(numOfElements == 0)
					break;
				concatenatedProps.append(queueElement[i]);
				numOfElements--;
			}
			for(int i=0; i<front; i++)
			{
				if(numOfElements == 0)
					break;
				concatenatedProps.append(queueElement[i]);
				numOfElements--;
			}
		}
		else
		{
			for(int i=rear; i<front; i++)
			{
				if(numOfElements == 0)
					break;
				concatenatedProps.append(queueElement[i]);
				numOfElements--;
			}
		}
		return concatenatedProps;
	}
}
