/*
 *
 * Copyright (C) 2008-2009 Yves Zoundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 *
 */
package net.sf.vfsjfilechooser.plaf.basic;

import net.sf.vfsjfilechooser.VFSJFileChooser;
import net.sf.vfsjfilechooser.constants.VFSJFileChooserConstants;
import net.sf.vfsjfilechooser.filechooser.AbstractVFSFileSystemView;
import net.sf.vfsjfilechooser.plaf.metal.MetalVFSFileChooserUI;
import net.sf.vfsjfilechooser.utils.FileObjectComparatorFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * The DirectoryModel implementation based on Swing BasicDirectoryModel
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>, Jason Harrop <jasonharrop at users.sourceforge.net>
 * @author Danita Sun <danita.sun at gmail.com>
 * @author Alex Arana <alex at arana.net.au>
 * @version 0.0.2
 */
@SuppressWarnings("serial")
public class BasicVFSDirectoryModel extends AbstractListModel
    implements PropertyChangeListener
{
    private static final Comparator<FileObject> fileNameComparator = FileObjectComparatorFactory.newFileNameComparator(true);
    private VFSJFileChooser filechooser = null;
    private final List<FileObject> fileCache = new ArrayList<FileObject>();
    private ReadWriteLock aLock = new ReentrantReadWriteLock(true);
    private volatile Future<?> loadThread = null;
    private ExecutorService executor;
    private List<FileObject> files = null;
    private List<FileObject> directories = null;
    private int fetchID = 0;
    private PropertyChangeSupport changeSupport;
    private boolean busy = false;

    /**
     *
     * @param filechooser
     */
    public BasicVFSDirectoryModel(VFSJFileChooser filechooser)
    {
        this.filechooser = filechooser;
        this.executor = Executors.newCachedThreadPool();
        validateFileCache();
    }

    /**
     *
     * @param e
     */
    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();

        if ((prop.equals(VFSJFileChooserConstants.DIRECTORY_CHANGED_PROPERTY)) ||
                (prop.equals(
                    VFSJFileChooserConstants.FILE_VIEW_CHANGED_PROPERTY)) ||
                (prop.equals(
                    VFSJFileChooserConstants.FILE_FILTER_CHANGED_PROPERTY)) ||
                (prop.equals(
                    VFSJFileChooserConstants.FILE_HIDING_CHANGED_PROPERTY)) ||
                (prop.equals(
                    VFSJFileChooserConstants.FILE_SELECTION_MODE_CHANGED_PROPERTY)))
        {
            validateFileCache();
        }
        else if ("UI".equals(prop))
        {
            Object old = e.getOldValue();

            if (old instanceof BasicVFSFileChooserUI)
            {
                BasicVFSFileChooserUI ui = (BasicVFSFileChooserUI) old;
                BasicVFSDirectoryModel model = ui.getModel();

                if (model != null)
                {
                    model.invalidateFileCache();
                }
            }
        }
        else if ("JFileChooserDialogIsClosingProperty".equals(prop))
        {
            invalidateFileCache();
        }
    }

    /**
     * This method is used to interrupt file loading thread.
     */
    public void invalidateFileCache()
    {
	    // Ensure loadThread is updated atomically.
		aLock.writeLock().lock();

	    try {
			if (loadThread != null)
			{
				loadThread.cancel(true);
				loadThread = null;
			}
	    }
	    finally {
		    aLock.writeLock().unlock();
	    }
    }

    /**
     *
     * @return
     */
    public List<FileObject> getFiles()
    {
        aLock.readLock().lock();

        try
        {
            if (files != null)
            {
                return files;
            }

            files = new CopyOnWriteArrayList<FileObject>();
            directories = new CopyOnWriteArrayList<FileObject>();

            FileObject currentDir = filechooser.getCurrentDirectory();
            AbstractVFSFileSystemView v = filechooser.getFileSystemView();
            directories.add(v.createFileObject(currentDir, ".."));

            for (FileObject f : fileCache)
            {
                if (filechooser.isTraversable(f))
                {
                    directories.add(f);
                }
                else
                {
                    files.add(f);
                }
            }

            return files;
        }
        finally
        {
            aLock.readLock().unlock();
        }
    }

    /**
     *
     */
    public void validateFileCache()
    {
        FileObject currentDirectory = filechooser.getCurrentDirectory();

        if (currentDirectory == null)
        {
            return;
        }

        try
        {
            currentDirectory.refresh();
        }
        catch (FileSystemException ex)
        {
        }

	    // Bug 2.
	    // Ensure that state is updated atomically.
	    aLock.writeLock().lock();

	    try {
			if (loadThread != null)
			{
				loadThread.cancel(true);
			}

            setBusy(true, ++fetchID);

		    // Give the LoadFilesThread access to its associated Future so that it LoadFilesThread
		    // can correctly determine if it has been cancelled.
		    // Previously, if the new LoadFilesThread executed to a point where it checked
		    // its cancel status before the loadThread variable was reassigned, the new LoadFilesThread
		    // could incorrectly think it had been cancelled (because it would read the status of the
		    // last, just cancelled LoadFilesThread), thus resulting in the new LoadFilesThread returning
		    // early and not adding the directory contents to the directory pane.
		    // In addition, previously, any cancelled LoadFilesThread would only get cancelled if it
		    // realised before the loadThread variable was reassigned.
		    final LoadFilesThread loader = new LoadFilesThread(currentDirectory, fetchID);
		    loadThread = executor.submit(loader);
		    loader.setTaskId(loadThread);
	    }
	    finally {
		    aLock.writeLock().unlock();
	    }
    }

    /**
     * Renames a file in the underlying file system.
     *
     * @param oldFile a <code>File</code> object representing
     *        the existing file
     * @param newFile a <code>File</code> object representing
     *        the desired new file name
     * @return <code>true</code> if rename succeeded,
     *        otherwise <code>false</code>
     * @since 1.4
     */
    public boolean renameFile(FileObject oldFile, FileObject newFile)
    {
        aLock.writeLock().lock();

        try
        {
            oldFile.moveTo(newFile);
            validateFileCache();

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            aLock.writeLock().unlock();
        }
    }

    /**
     *
     */
    public void fireContentsChanged()
    {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public int getSize()
    {
        return fileCache.size();
    }

    /**
     *
     * @param o
     * @return
     */
    public boolean contains(Object o)
    {
        return fileCache.contains(o);
    }

    /**
     * @param o
     * @return
     */
    public int indexOf(Object o)
    {
        return fileCache.indexOf(o);
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int index)
    {
        return fileCache.get(index);
    }

    /**
     * @param comparator
     */
    public void sort(Comparator<FileObject> comparator)
    {
        Collections.sort(fileCache, comparator);
    }

    /**
     *
     * @param v
     */
    protected void sort(List<FileObject> v)
    {
        Collections.sort(v, fileNameComparator);
    }

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is
     * registered for all bound properties of this class.
     * <p>
     * If <code>listener</code> is <code>null</code>,
     * no exception is thrown and no action is performed.
     *
     * @param    listener  the property change listener to be added
     *
     * @see #removePropertyChangeListener
     * @see #getPropertyChangeListeners
     *
     * @since 1.6
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (changeSupport == null)
        {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param listener the PropertyChangeListener to be removed
     *
     * @see #addPropertyChangeListener
     * @see #getPropertyChangeListeners
     *
     * @since 1.6
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (changeSupport != null)
        {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Returns an array of all the property change listeners
     * registered on this component.
     *
     * @return all of this component's <code>PropertyChangeListener</code>s
     *         or an empty array if no property change
     *         listeners are currently registered
     *
     * @see      #addPropertyChangeListener
     * @see      #removePropertyChangeListener
     * @see      java.beans.PropertyChangeSupport#getPropertyChangeListeners
     *
     * @since 1.6
     */
    public PropertyChangeListener[] getPropertyChangeListeners()
    {
        if (changeSupport == null)
        {
            return new PropertyChangeListener[0];
        }

        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Support for reporting bound property changes for boolean properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     *
     * @since 1.6
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
        Object newValue)
    {
        if (changeSupport != null)
        {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Set the busy state for the model. The model is considered
     * busy when it is running a separate (interruptable)
     * thread in order to load the contents of a directory.
     */
    private void setBusy(final boolean busy, int fid)
    {
        aLock.writeLock().lock();

        try
        {
            if (fid == fetchID)
            {
                boolean oldValue = this.busy;
                this.busy = busy;

                if ((changeSupport != null) && (busy != oldValue))
                {
                    Runnable r = (new Runnable()
                        {
                            public void run()
                            {
                                firePropertyChange("busy", !busy, busy);
                            }
                        });

                    if (SwingUtilities.isEventDispatchThread())
                    {
                        r.run();
                    }
                    else
                    {
                        SwingUtilities.invokeLater(r);
                    }
                }
            }
        }
        finally
        {
            aLock.writeLock().unlock();
        }
    }

    class LoadFilesThread implements Runnable
    {
        private int fid;
	    private Future<?> taskId = null;
        private Queue<DoChangeContents> runnables = new ConcurrentLinkedQueue<DoChangeContents>();

        public LoadFilesThread(FileObject currentDirectory, int fid)
        {
            this.fid = fid;
        }

	    void setTaskId(Future<?> taskId)
	    {
		    aLock.writeLock().lock();

		    try {
			    this.taskId = taskId;
		    }
		    finally {
			    aLock.writeLock().unlock();
		    }
	    }

        private void invokeLater(DoChangeContents runnable)
        {
            runnables.add(runnable);

            if (SwingUtilities.isEventDispatchThread())
            {
                runnable.run();
            }
            else
            {
                SwingUtilities.invokeLater(runnable);
            }
        }


	    // Bug 2.
	    private boolean isCancelled()
	    {
		    aLock.readLock().lock();

		    try {
			    return taskId != null && taskId.isCancelled();
		    }
		    finally {
			    aLock.readLock().unlock();
		    }
	    }


        public void run()
        {
            run0();
            setBusy(false, fid);
        }

        public void run0()
        {
	        if (isCancelled())
	        {
	            return;
	        }

          // fix a bug here when the filesystem changes, the directories list needs to be notified
	        // Invoke setSelectedItem() off the swing event thread as it modifies GUI state.
	        // This prevents the selected directory combo box from sometimes appearing empty
	        // when displayed.
	        SwingUtilities.invokeLater(new Runnable()
	        {
		        public void run()
		        {
			        boolean update = false;
			        final MetalVFSFileChooserUI ui = (MetalVFSFileChooserUI) filechooser.getUI();
			        final FileObject currentDirectory;

			        aLock.readLock().lock();

			        try {
				        currentDirectory = filechooser.getCurrentDirectory();

				        if (!contains(currentDirectory)) {
					        // Only update the UI if we haven't been cancelled.  This prevents the case where
					        // the file chooser opens in the wrong directory, which is possible if a later
					        // instance of LoadThread queues this update before an older instance that has not
					        // yet seen that it has been cancelled.  Note that it is not necessary to
					        // ensure that the setSelectedItem() happens while we hold the read lock
					        // because this update must complete before the next.
				            if (!isCancelled() && (fetchID == fid)) {
					            update = true;
				            }
			            }
			        }
			        finally {
				        aLock.readLock().unlock();
			        }

			        if (update) {
					    ui.getCombo().setSelectedItem(currentDirectory);
			        }
		        }
	        });

	        AbstractVFSFileSystemView fileSystem = filechooser.getFileSystemView();

	        final FileObject cwd = filechooser.getCurrentDirectory();

            FileObject[] list = fileSystem.getFiles(cwd,
                    filechooser.isFileHidingEnabled());

            List<FileObject> acceptsList = new ArrayList<FileObject>(list.length);

	        if (isCancelled())
            {
                return;
            }

            // run through the file list, add directories and selectable files to fileCache
            for (FileObject aFileObject : list)
            {
                if (filechooser.accept(aFileObject))
                {
                    acceptsList.add(aFileObject);
                }
            }

	        if (isCancelled())
            {
                cancelRunnables();

                return;
            }

            // First sort alphabetically by filename
            sort(acceptsList);

            final int mid = acceptsList.size() >> 1;

            List<FileObject> newDirectories = new ArrayList<FileObject>(mid);
            List<FileObject> newFiles = new ArrayList<FileObject>(mid);

            // run through list grabbing directories in chunks of ten
            for (FileObject f : acceptsList)
            {
                boolean isTraversable = filechooser.isTraversable(f);

                if (isTraversable)
                {
                    newDirectories.add(f);
                }
                else
                {
                    newFiles.add(f);
                }

	            if (isCancelled())
                {
                    cancelRunnables();

                    return;
                }
            }

            List<FileObject> newFileCache = new ArrayList<FileObject>(newDirectories);
            newFileCache.addAll(newFiles);

            int newSize = newFileCache.size();
            int oldSize = fileCache.size();

            if (newSize > oldSize)
            {
                //see if interval is added
                int start = oldSize;
                int end = newSize;

                for (int i = 0; i < oldSize; i++)
                {
                    if (!newFileCache.get(i).equals(fileCache.get(i)))
                    {
                        start = i;

                        for (int j = i; j < newSize; j++)
                        {
                            if (newFileCache.get(j).equals(fileCache.get(i)))
                            {
                                end = j;

                                break;
                            }
                        }

                        break;
                    }
                }

                if ((start >= 0) && (end > start) &&
                        newFileCache.subList(end, newSize)
                                        .equals(fileCache.subList(start, oldSize)))
                {
	                if (isCancelled())
                    {
                        cancelRunnables();

                        return;
                    }

                    invokeLater(new DoChangeContents(newFileCache.subList(
                                start, end), start, null, 0, fid));
                    newFileCache = null;
                }
            }
            else if (newSize < oldSize)
            {
                //see if interval is removed
                int start = -1;
                int end = -1;

                for (int i = 0; i < newSize; i++)
                {
                    if (!newFileCache.get(i).equals(fileCache.get(i)))
                    {
                        start = i;
                        end = (i + oldSize) - newSize;

                        break;
                    }
                }

                if ((start >= 0) && (end > start) &&
                        fileCache.subList(end, oldSize)
                                     .equals(newFileCache.subList(start, newSize)))
                {
	                if (isCancelled())
                    {
                        cancelRunnables(runnables);

                        return;
                    }

                    invokeLater(new DoChangeContents(null, 0,
                            new ArrayList<FileObject>(fileCache.subList(start,
                                    end)), start, fid));
                    newFileCache = null;
                }
            }

            if ((newFileCache != null) && !fileCache.equals(newFileCache))
            {
	            if (isCancelled())
                {
                    cancelRunnables(runnables);

                    return;
                }

                invokeLater(new DoChangeContents(newFileCache, 0, fileCache, 0,
                        fid));
            }
        }

        public void cancelRunnables(Queue<DoChangeContents> runnables)
        {
            DoChangeContents runnable = null;

            while ((runnable = runnables.poll()) != null)
            {
	            runnable.cancel();
            }
        }

        public void cancelRunnables()
        {
            cancelRunnables(runnables);
        }
    }

    class DoChangeContents implements Runnable
    {
        private List<FileObject> addFiles;
        private List<FileObject> remFiles;
        private boolean doFire = true;
        private int fid;
        private int addStart = 0;
        private int remStart = 0;

        public DoChangeContents(List<FileObject> addFiles, int addStart,
            List<FileObject> remFiles, int remStart, int fid)
        {
            this.addFiles = addFiles;
            this.addStart = addStart;
            this.remFiles = remFiles;
            this.remStart = remStart;
            this.fid = fid;
        }

        void cancel()
        {
            aLock.writeLock().lock();

            try
            {
                doFire = false;
            }
            finally
            {
                aLock.writeLock().unlock();
            }
        }

        public void run()
        {
	        // Ensure all state variables are synchronised.
	        int remSize = 0;
	        int addSize = 0;
	        boolean modified = false;

	        aLock.writeLock().lock();

	        try {
				if ((fetchID == fid) && doFire)
				{
					remSize = (remFiles == null) ? 0 : remFiles.size();
					addSize = (addFiles == null) ? 0 : addFiles.size();

					if (remSize > 0) {
						fileCache.removeAll(remFiles);
					}

					if (addSize > 0) {
						fileCache.addAll(addStart, addFiles);
					}

					files = null;
					directories = null;

					modified = true;
				}
	        }
	        finally {
		        aLock.writeLock().unlock();
	        }

	        if (modified && (remSize > 0) && (addSize == 0))
			{
				fireIntervalRemoved(BasicVFSDirectoryModel.this, remStart,
					(remStart + remSize) - 1);
			}
			else if (modified && (addSize > 0) && (remSize == 0) &&
				(fileCache.size() > addSize))
			{
				fireIntervalAdded(BasicVFSDirectoryModel.this, addStart,
					(addStart + addSize) - 1);
			}
			else if (modified)
			{
				fireContentsChanged();
			}
        }
    }
}
