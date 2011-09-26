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
 */
package net.sf.vfsjfilechooser.accessories.bookmarks;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import net.sf.vfsjfilechooser.VFSJFileChooser;
import net.sf.vfsjfilechooser.utils.VFSResources;
import net.sf.vfsjfilechooser.utils.VFSUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.ftp.FtpFileSystemConfigBuilder;


/**
 * Bookmarks manager panel
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @author Jojada Tirtowidjojo <jojada at users.sourceforge.net>
 * @author Alex Arana <alex at arana.net.au>
 * @version 0.0.1
 */
@SuppressWarnings("serial")
public class BookmarksManagerPanel extends JPanel
{
    public static final int NO_BOOKMARK_SELECTION_INDEX = -1;
    private JScrollPane scrollPane;
    private JTable table;
    private JButton bOpen;
    private JButton bCancel;
    private JButton bEdit;
    private JButton bInlineEdit;
    private JButton bAdd;
    private JButton bDelete;
    private JButton bMoveUp;
    private JButton bMoveDown;
    private Bookmarks model;
    private VFSJFileChooser chooser;
    private final Dimension tableSize = new Dimension(350, 200);
    private BookmarksDialog parentDialog;

    private final FileSystemOptions ftpFileOptions = new FileSystemOptions();

    public BookmarksManagerPanel(BookmarksDialog parentDialog,
        VFSJFileChooser chooser)
    {
        this.parentDialog = parentDialog;
        this.chooser = chooser;

        model = new Bookmarks();

        table = new JTable(model);
        scrollPane = new JScrollPane(table);

        table.setPreferredScrollableViewportSize(tableSize);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        bCancel = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.closeButtonText"));

        bOpen = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.openButtonText"));
        bOpen.setIcon(new ImageIcon(getClass()
                                        .getResource("/net/sf/vfsjfilechooser/plaf/icons/document-open.png")));
        bOpen.setHorizontalAlignment(SwingConstants.LEFT);

        bAdd = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.addButtonText"));
        bAdd.setIcon(new ImageIcon(getClass()
                                       .getResource("/net/sf/vfsjfilechooser/plaf/icons/list-add.png")));
        bAdd.setHorizontalAlignment(SwingConstants.LEFT);

        bEdit = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.editButtonText"));
        bEdit.setIcon(new ImageIcon(getClass()
                                        .getResource("/net/sf/vfsjfilechooser/plaf/icons/book_edit.png")));
        bEdit.setHorizontalAlignment(SwingConstants.LEFT);

        bInlineEdit = new JButton("...");

        bDelete = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.deleteButtonText"));
        bDelete.setIcon(new ImageIcon(getClass()
                                          .getResource("/net/sf/vfsjfilechooser/plaf/icons/list-remove.png")));
        bDelete.setHorizontalAlignment(SwingConstants.LEFT);

        bMoveUp = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.moveUpButtonText"));
        bMoveUp.setIcon(new ImageIcon(getClass()
                                          .getResource("/net/sf/vfsjfilechooser/plaf/icons/go-up.png")));
        bMoveUp.setHorizontalAlignment(SwingConstants.LEFT);

        bMoveDown = new JButton(VFSResources.getMessage(
                    "VFSJFileChooser.moveDownButtonText"));
        bMoveDown.setIcon(new ImageIcon(getClass()
                                            .getResource("/net/sf/vfsjfilechooser/plaf/icons/go-down.png")));
        bMoveDown.setHorizontalAlignment(SwingConstants.LEFT);

        final ActionHandler ah = new ActionHandler();

        bOpen.addActionListener(ah);
        bCancel.addActionListener(ah);
        bEdit.addActionListener(ah);
        bInlineEdit.addActionListener(ah);
        bAdd.addActionListener(ah);
        bDelete.addActionListener(ah);
        bMoveUp.addActionListener(ah);
        bMoveDown.addActionListener(ah);

        final Box south = Box.createHorizontalBox();
        south.add(Box.createHorizontalGlue());
        south.add(bCancel);
        south.add(Box.createHorizontalGlue());

        final JPanel buttons = new JPanel(new GridLayout(0, 1, 3, 3));

        buttons.add(bAdd);
        buttons.add(bEdit);
        buttons.add(bDelete);
        buttons.add(bOpen);
        buttons.add(Box.createVerticalStrut(10));
        buttons.add(bMoveUp);
        buttons.add(bMoveDown);

        setLayout(new BorderLayout(10, 3));

        add(scrollPane, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        add(buttons, BorderLayout.EAST);

        BookmarkCellEditor b = new BookmarkCellEditor(bInlineEdit);
        table.setDefaultEditor(String.class, b);
        table.setDefaultEditor(Object.class, b);
        table.setCellEditor(b);

        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10,
                UIManager.getColor("Panel.background")));
    }

    public Bookmarks getModel()
    {
        return model;
    }

    public void cancel()
    {
        parentDialog.setVisible(false);
    }

    private class ActionHandler implements ActionListener
    {
        public void actionPerformed(ActionEvent actionEvent)
        {
            JButton button = (JButton) actionEvent.getSource();

            final int row = table.getSelectedRow();

            if (button.equals(bAdd))
            {
                parentDialog.showEditorView(NO_BOOKMARK_SELECTION_INDEX);
            }
            else if (button.equals(bEdit) || button.equals(bInlineEdit))
            {
                if (row != NO_BOOKMARK_SELECTION_INDEX)
                {
                    parentDialog.showEditorView(row);
                }
                else
                {
                    JOptionPane.showMessageDialog(getParent(),
                        VFSResources.getMessage(
                            "VFSJFileChooser.noselectionLabel"),
                        VFSResources.getMessage(
                            "VFSJFileChooser.errorLabel"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
            else if (button.equals(bOpen))
            {
                if (row != NO_BOOKMARK_SELECTION_INDEX)
                {
                    Thread worker = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                setCursor(Cursor.getPredefinedCursor(
                                        Cursor.WAIT_CURSOR));

                                TitledURLEntry aTitledURLEntry = model.getEntry(row);

                                FileObject fo = null;

                                try
                                {
                                    if (aTitledURLEntry instanceof FTPURLEntry)
                                    {
                                        FTPURLEntry ftpEntry = (FTPURLEntry) aTitledURLEntry;
                                        FtpFileSystemConfigBuilder.getInstance()
                                            .setPassiveMode(ftpFileOptions, ftpEntry.isPassiveFtp());
                                        fo = VFSUtils.resolveFileObject(aTitledURLEntry.getURL(), ftpFileOptions);
                                    }
                                    else
                                    {
                                        fo = VFSUtils.resolveFileObject(aTitledURLEntry.getURL());
                                    }

                                    if ((fo != null) && !fo.exists())
                                    {
                                        fo = null;
                                    }
                                }
                                catch (Exception exc)
                                {
                                    fo = null;
                                }

                                setCursor(Cursor.getDefaultCursor());

                                if (fo == null)
                                {
                                    StringBuilder msg = new StringBuilder();
                                    msg.append("Failed to connect to ");
                                    msg.append(aTitledURLEntry.getURL());
                                    msg.append("\n");
                                    msg.append(
                                        "Please check URL entry and try again.");
                                    JOptionPane.showMessageDialog(null, msg,
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                else
                                {
                                    chooser.setCurrentDirectory(fo);
                                    parentDialog.setVisible(false);
                                }
                            }
                        };

                    worker.setPriority(Thread.MIN_PRIORITY);
                    SwingUtilities.invokeLater(worker);
                }
                else
                {
                    JOptionPane.showMessageDialog(getParent(),
                        VFSResources.getMessage(
                            "VFSJFileChooser.noselectionLabel"),
                        VFSResources.getMessage(
                            "VFSJFileChooser.errorLabel"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
            else if (button.equals(bDelete))
            {
                int[] rows = table.getSelectedRows();

                if (rows.length == 0)
                {
                    JOptionPane.showMessageDialog(getParent(),
                        VFSResources.getMessage(
                            "VFSJFileChooser.noselectionLabel"),
                        VFSResources.getMessage(
                            "VFSJFileChooser.errorLabel"),
                        JOptionPane.ERROR_MESSAGE);

                    return;
                }
                else
                {
                    for (int i = rows.length - 1; i >= 0; i--)
                    {
                        model.delete(rows[i]);
                    }
                }
            }
            else if (button.equals(bMoveUp))
            {
                int[] rows = table.getSelectedRows();

                if (rows.length == 0)
                {
                    JOptionPane.showMessageDialog(getParent(),
                        VFSResources.getMessage(
                            "VFSJFileChooser.noselectionLabel"),
                        VFSResources.getMessage(
                            "VFSJFileChooser.errorLabel"),
                        JOptionPane.ERROR_MESSAGE);

                    return;
                }
                else if (rows.length > 1)
                {
                    return;
                }
                else if (rows[0] > 0)
                {
                    model.moveup(rows[0]);
                    table.setRowSelectionInterval(rows[0] - 1, rows[0] - 1);
                }
            }
            else if (button.equals(bMoveDown))
            {
                int[] rows = table.getSelectedRows();

                if (rows.length == 0)
                {
                    JOptionPane.showMessageDialog(getParent(),
                        VFSResources.getMessage(
                            "VFSJFileChooser.noselectionLabel"),
                        VFSResources.getMessage(
                            "VFSJFileChooser.errorLabel"),
                        JOptionPane.ERROR_MESSAGE);

                    return;
                }
                else if (rows.length > 1)
                {
                    return;
                }
                else if (rows[0] < (model.getRowCount() - 1))
                {
                    model.movedown(rows[0]);
                    table.setRowSelectionInterval(rows[0] + 1, rows[0] + 1);
                }
            }
            else if (button.equals(bCancel))
            {
                cancel();
            }
        }
    } // inner class ActionHandler

    public static class BookmarkCellEditor extends DefaultCellEditor
    {
        JPanel p = new JPanel();
        JButton b;
        JTextField tf;

        public BookmarkCellEditor(JButton b)
        {
            super(new JTextField());
            tf = (JTextField) editorComponent;
            tf.setEditable(true);
            p.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();

            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridheight = 1;
            c.gridwidth = 1;
            c.weightx = 1.0;
            c.ipadx = 0;
            c.insets = new Insets(0, 0, 0, 0);
            p.add(tf, c);
            
            c.gridx = 1;
            c.anchor = GridBagConstraints.LINE_END;
            c.fill = GridBagConstraints.NONE;
            c.gridheight = 1;
            c.gridwidth = 1;
            c.weightx = 0.0;
            p.add(b, c);
        }

        @Override
        public Component getComponent()
        {
            return p;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            if (value instanceof String)
            {
                tf.setText((String)value);
            }
            return getComponent();
        }

        @Override
        public Object getCellEditorValue() {
            return tf.getText();
        }


    }
}
