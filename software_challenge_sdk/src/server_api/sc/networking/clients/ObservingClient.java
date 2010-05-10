package sc.networking.clients;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sc.protocol.responses.ErrorResponse;
import sc.shared.GameResult;

import com.thoughtworks.xstream.XStream;

public class ObservingClient implements IControllableGame, IHistoryListener
{
	public ObservingClient(XStream xStream, InputStream inputStream)
			throws IOException
	{
		this.poller = new ReplayClient(xStream, inputStream);
		this.roomId = null;
		this.poller.addListener(this);
		this.mode = PlayMode.PAUSED;
		this.replay = true;
		this.poller.start();
	}

	public ObservingClient(IPollsHistory client, String roomId)
	{
		this.poller = client;
		this.roomId = roomId;
		this.poller.addListener(this);
		this.replay = false;
	}

	protected final IPollsHistory		poller;

	protected final String				roomId;

	private static final Logger			logger		= LoggerFactory
															.getLogger(ObservingClient.class);

	private final boolean				replay;

	private boolean						gameOver	= false;

	private final List<Object>			history		= new LinkedList<Object>();

	private final List<IUpdateListener>	listeners	= new LinkedList<IUpdateListener>();

	protected int						position	= 0;

	protected PlayMode					mode		= PlayMode.PAUSED;

	private GameResult					result		= null;

	enum PlayMode
	{
		PLAYING, PAUSED
	}

	protected void addObservation(Object observation)
	{
		boolean firstObservation = this.history.isEmpty();

		this.history.add(observation);

		if (canAutoStep() || firstObservation)
		{
			setPosition(this.history.size() - 1);
		}
	}

	private boolean canAutoStep()
	{
		return !this.replay || this.mode == PlayMode.PLAYING;
	}

	@Override
	public void onNewState(String roomId, Object state)
	{
		if (isAffected(roomId))
		{
			addObservation(state);
		}
	}

	private boolean isAffected(String roomId)
	{
		return this.replay || this.roomId.equals(roomId);
	}

	protected void notifyOnUpdate()
	{
		for (IUpdateListener listener : this.listeners)
		{
			listener.onUpdate(this);
		}
	}
	
	protected void notifyOnError(String errorMessage)
	{
		for (IUpdateListener listener : this.listeners)
		{
			listener.onError(errorMessage);
		}
	}

	@Override
	public void removeListener(IUpdateListener u)
	{
		this.listeners.remove(u);
	}

	@Override
	public void addListener(IUpdateListener u)
	{
		this.listeners.add(u);
		u.onUpdate(this);
	}

	@Override
	public void next()
	{
		this.changePosition(+1);
	}

	@Override
	public void pause()
	{
		this.mode = PlayMode.PAUSED;
		notifyOnUpdate();
	}

	@Override
	public void previous()
	{
		if (!isPaused())
		{
			pause();
		}

		this.changePosition(-1);
	}

	protected void changePosition(int i)
	{
		this.setPosition(this.getPosition() + i);
	}

	private int getPosition()
	{
		return this.position;
	}

	@Override
	public void unpause()
	{
		this.mode = PlayMode.PLAYING;

		if (this.replay)
		{
			next();
		}
		else
		{
			this.setPosition(this.history.size() - 1);
		}
	}

	protected void setPosition(int i)
	{
		logger.debug("Setting Position to {}", i);
		this.position = Math.max(0, Math.min(this.history.size() - 1, i));
		notifyOnUpdate();
	}

	public Object getCurrentState()
	{
		if (this.history.size() == 0)
		{
			return null;
		}

		return this.history.get(this.position);
	}

	@Override
	public boolean isAtStart()
	{
		return this.getPosition() == 0;
	}

	@Override
	public boolean isAtEnd()
	{
		return this.getPosition() >= this.history.size() - 1;
	}

	public List<Object> getHistory()
	{
		return Collections.unmodifiableList(this.history);
	}

	@Override
	public boolean hasNext()
	{
		return (this.getPosition() + 1) < this.history.size();
	}

	@Override
	public boolean hasPrevious()
	{
		return this.getPosition() > 0;
	}

	@Override
	public boolean isPaused()
	{
		return this.mode == PlayMode.PAUSED;
	}

	public boolean isGameOver()
	{
		return this.replay || this.gameOver;
	}

	public void close()
	{
		this.history.clear();
	}

	@Override
	public void onGameOver(String roomId, GameResult result)
	{
		if (this.result != null)
		{
			logger.warn("Received two GameResults");
		}

		this.gameOver = true;
		this.result = result;
		
		notifyOnUpdate();
	}

	@Override
	public void cancel()
	{
		this.mode = PlayMode.PAUSED;
	}

	@Override
	public void goToFirst()
	{
		this.setPosition(0);
	}

	@Override
	public void goToLast()
	{
		this.setPosition(getHistory().size() - 1);
	}

	@Override
	public boolean canTogglePause()
	{
		return false;
	}

	public GameResult getResult()
	{
		return this.result;
	}

	@Override
	public boolean isReplay()
	{
		return this.replay;
	}

	@Override
	public void onGameError(ErrorResponse error)
	{
		if (isAffected(null))
		{
			addObservation(error);
		}
	}
}
