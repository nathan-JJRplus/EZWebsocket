// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package processqueue.actions;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.webui.CustomJavaAction;
import processqueue.proxies.QueuedAction;
import processqueue.queuehandler.QueueHandler;

/**
 * Append the new action to the Queue, based on the configured process the action will be appended to the correct Queue.
 * The action should be initialized with its default values, you should only change the RefrenceText for your own reference. Optionally you can set the association to the Process entity.
 * 
 * Either the association between action and Process must be filled or you should provide an input parameter. 
 * Of course the input parameter is much faster since the Java doesn't need to retrieve anything from the database. 
 */
public class AppendNewActionToQueue extends CustomJavaAction<java.lang.Boolean>
{
	private IMendixObject __ActionToQueue;
	private processqueue.proxies.QueuedAction ActionToQueue;
	private IMendixObject __AddActionToProcess;
	private processqueue.proxies.Process AddActionToProcess;

	public AppendNewActionToQueue(IContext context, IMendixObject ActionToQueue, IMendixObject AddActionToProcess)
	{
		super(context);
		this.__ActionToQueue = ActionToQueue;
		this.__AddActionToProcess = AddActionToProcess;
	}

	@java.lang.Override
	public java.lang.Boolean executeAction() throws Exception
	{
		this.ActionToQueue = this.__ActionToQueue == null ? null : processqueue.proxies.QueuedAction.initialize(getContext(), __ActionToQueue);

		this.AddActionToProcess = this.__AddActionToProcess == null ? null : processqueue.proxies.Process.initialize(getContext(), __AddActionToProcess);

		// BEGIN USER CODE
		IContext context = Core.createSystemContext();
		
		IMendixObject process = this.__AddActionToProcess;
		IMendixIdentifier processId = this.__ActionToQueue.getValue(context, QueuedAction.MemberNames.QueuedAction_Process.toString());
		
		if( process == null ) {
			if( processId != null ) { 
				process = Core.retrieveId(context, processId);
			}
			else
				throw new CoreException("No process specified for Queued object: " + this.ActionToQueue.getActionNumber(context) );
		}
		else if( processId == null ) {
				this.__ActionToQueue.setValue(context, QueuedAction.MemberNames.QueuedAction_Process.toString(), this.__AddActionToProcess.getId());
		}

		//Make sure we commit the latest info about the action before passing it along
		if( this.__ActionToQueue.isNew() || this.__ActionToQueue.isChanged() ) 
			Core.commit(getContext(), this.__ActionToQueue);
	
		QueueHandler.getQueueHandler().appendActionForProcessing(context,  this.__ActionToQueue, process, false);
		
		return true;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 * @return a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "AppendNewActionToQueue";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}