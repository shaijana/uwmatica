package fi.dy.masa.litematica.schematic.placement;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;

public class PlacementManagerDaemonExecutor implements IThreadDaemonExecutor<PlacementManagerTask>
{
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final AtomicBoolean paused = new AtomicBoolean(false);
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition hasTasks = this.lock.newCondition();
	private final long sleepTime;
	private final float sleepDelay;
	private long lastTaskTime;

	public PlacementManagerDaemonExecutor()
	{
		this(600000L);  // 10 min
	}

	public PlacementManagerDaemonExecutor(long sleepTime)
	{
		this.sleepTime = MathUtils.clamp(sleepTime, 60000L, Long.MAX_VALUE); // 1 min
		this.sleepDelay = 10.0F;     // 10-second sleep delay
	}

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	public boolean isPaused()
	{
		return this.paused.get();
	}

	@Override
	public void start()
	{
		if (!this.isRunning())
		{
			Litematica.debugLogError("Executor: Starting");
			if (this.isPaused())
			{
				this.paused.set(false);
			}

			this.running.set(true);
		}

		if (this.hasTasks())
		{
			this.signalHasTasks();
		}

		this.run();
	}

	@Override
	public void interrupt(InterruptedException interrupt)
	{
		Litematica.debugLogError("Executor: Interrupt Signal: {}",
		                        interrupt.getLocalizedMessage() != null
		                        ? interrupt.getLocalizedMessage()  // This is null sometimes?
		                        : "received interrupt signal");
		if (this.isPaused() || !this.isRunning())
		{
			this.resume();
		}

		if (this.hasTasks())
		{
			this.signalHasTasks();
		}
	}

	@Override
	public void pause()
	{
		Litematica.debugLogError("Executor: Pausing");
		this.paused.set(true);
	}

	@Override
	public void resume()
	{
		if (this.isPaused())
		{
			Litematica.debugLogError("Executor: Resuming");
			this.paused.set(false);
		}

		this.start();
	}

	@Override
	public void stop()
	{
		Litematica.debugLogError("Executor: Stopping");
		if (!this.isPaused())
		{
			this.paused.set(true);
		}
		if (this.isRunning())
		{
			this.running.set(false);
		}
	}

	@Override
	public long sleepTime()
	{
		return this.sleepTime;
	}

	@Override
	public String getName()
	{
		return PlacementManagerDaemonHandler.INSTANCE.getName();
	}

	@Override
	public boolean hasTasks()
	{
		return PlacementManagerDaemonHandler.INSTANCE.hasTasks();
	}

	@Override
	public void run()
	{
		if (!this.isCorrectThread()) { return; }
		this.lastTaskTime = System.currentTimeMillis();
		Litematica.debugLogError("Executor: Running: [{}/{}]", this.isRunning(), this.isPaused());

		while (this.isRunning())
		{
			if (this.isPaused() && this.hasTasks())
			{
				this.resume();
			}
			else if (!this.isPaused() && this.loopSafe())
			{
				this.paused.set(true);
				this.sleep();
				return;
			}
		}
	}

	@Override
	public boolean loopSafe()
	{
		try
		{
			PlacementManagerTask task = this.takeNextTask();

			if (task != null)
			{
				this.processTask(task);
				this.lastTaskTime = System.currentTimeMillis();
				return false;
			}
		}
		catch (InterruptedException e)
		{
			this.interrupt(e);
		}
		catch (Exception err)
		{
			Litematica.debugLogError("PlacementManagerDaemonExecutor#loopSafe: Exception: {}", err.getLocalizedMessage());
		}

		return this.shouldPause();
	}

	@Override
	public boolean shouldPause()
	{
//		if (this.hasTasks()) { return false; }
//		return (System.currentTimeMillis() - this.lastTaskTime) > (this.sleepDelay * 1000L);
		return !this.hasTasks();
	}

	private void signalHasTasks()
	{
		Litematica.debugLogError("Executor: Signal Has Tasks");
		final ReentrantLock lock = this.lock;
		lock.lock();

		try
		{
			this.hasTasks.signal();
		}
		finally
		{
			lock.unlock();
		}
	}

	private PlacementManagerTask takeNextTask() throws InterruptedException
	{
		final PlacementManagerTask task;
		final int cx;
		final AtomicInteger count = new AtomicInteger(PlacementManagerDaemonHandler.INSTANCE.getTaskCount());
		final ReentrantLock lock = this.lock;

		lock.lockInterruptibly();

		try
		{
			while (count.get() == 0)
			{
				this.hasTasks.await();
			}

			task = PlacementManagerDaemonHandler.INSTANCE.getNextTask();
			cx = count.getAndDecrement();

			if (cx > 1)
			{
				this.hasTasks.signal();
			}
		}
		finally
		{
			lock.unlock();
		}

		return task;
	}

	@Override
	public void processTask(PlacementManagerTask task) throws InterruptedException
	{
		task.run();
	}
}
