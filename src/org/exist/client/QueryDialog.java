/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.xml.transform.OutputKeys;

import org.exist.xmldb.XPathQueryServiceImpl;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

public class QueryDialog extends JFrame {

    private InteractiveClient client;
	private Collection collection;
	private Properties properties;
	private ClientTextArea query;
	private ClientTextArea resultDisplay;
	private JComboBox collections= null;
	private SpinnerNumberModel count;
	private DefaultComboBoxModel history= new DefaultComboBoxModel();
    private Font display = new Font("Monospaced", Font.BOLD, 12);
	private JTextField statusMessage;
	private JProgressBar progress;

	public QueryDialog(InteractiveClient client, Collection collection, Properties properties) {
		this.collection= collection;
		this.properties= properties;
        this.client = client;
		setupComponents();
		pack();
	}

	private void setupComponents() {
		getContentPane().setLayout(new BorderLayout());
		JToolBar toolbar = new JToolBar();
		
		URL url= getClass().getResource("icons/Open24.gif");
		JButton button= new JButton(new ImageIcon(url));
		button.setToolTipText(
		"Read query from file.");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		toolbar.add(button);
		
		url= getClass().getResource("icons/SaveAs24.gif");
		button= new JButton(new ImageIcon(url));
		button.setToolTipText(
		"Write query to file.");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		toolbar.add(button);
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setResizeWeight(0.5);
		
		JComponent qbox= createQueryBox();
		split.setTopComponent(qbox);

        JPanel vbox = new JPanel();
        vbox.setLayout(new BorderLayout());
        
        JLabel label = new JLabel("Results:");
        vbox.add(label, BorderLayout.NORTH);
        
		resultDisplay= new ClientTextArea(false, "XML");
		resultDisplay.setText("");
		resultDisplay.setPreferredSize(new Dimension(400, 250));
        vbox.add(resultDisplay, BorderLayout.CENTER);
        
        Box statusbar = Box.createHorizontalBox();
        statusbar.setBorder(BorderFactory
        		.createBevelBorder(BevelBorder.LOWERED));
        statusMessage = new JTextField(20);
        statusMessage.setEditable(false);
        statusMessage.setFocusable(false);
        statusbar.add(statusMessage);

        progress = new JProgressBar();
        progress.setPreferredSize(new Dimension(200, 30));
        progress.setVisible(false);
        statusbar.add(progress);
        
        vbox.add(statusbar, BorderLayout.SOUTH);
        
		split.setBottomComponent(vbox);
		split.setDividerLocation(0.4);
		getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
	}

	private JComponent createQueryBox() {
		JPanel inputVBox = new JPanel();
		inputVBox.setLayout(new BorderLayout());
		
		Box historyBox= Box.createHorizontalBox();
		JLabel label= new JLabel("History: ");
		historyBox.add(label);
		final JComboBox historyList= new JComboBox(history);
		for(Iterator i = client.queryHistory.iterator(); i.hasNext(); ) {
			String item = (String) i.next();
			if(item.length() > 40)
				item = item.substring(0, 40);
			history.addElement(item);
		}
		historyList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String item = (String)client.queryHistory.get(historyList.getSelectedIndex());
				query.setText(item);
			}
		});
		historyBox.add(historyList);
		inputVBox.add(historyBox, BorderLayout.NORTH);
        
        JPanel queryPanel = new JPanel();
        queryPanel.setLayout(new BorderLayout());
        label = new JLabel("XQuery:");
        queryPanel.add(label, BorderLayout.NORTH);
        
        query = new ClientTextArea(true, "XQUERY");
        query.setElectricScroll(1);
		query.setEditable(true);
		query.setPreferredSize(new Dimension(350, 200));
		queryPanel.add(query, BorderLayout.CENTER);
        inputVBox.add(queryPanel, BorderLayout.CENTER);
        
		Box optionsPanel = Box.createHorizontalBox();
        
        label = new JLabel("Context:");
        optionsPanel.add(label);
        
		final Vector data= new Vector();
		try {
			Collection root = client.getCollection("/db");
			data.addElement(collection.getName());
			getCollections(root, collection, data);
		} catch (XMLDBException e) {
			ClientFrame.showErrorMessage(
					"An error occurred while retrieving collections list.", e);
		}
		collections= new JComboBox(data);
		collections.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int p = collections.getSelectedIndex();
				String context = (String)data.elementAt(p);
				try {
					collection = client.getCollection(context);
				} catch (XMLDBException e1) {
				}
			}
		});
        optionsPanel.add(collections);

		label= new JLabel("Display max.:");
        optionsPanel.add(label);
        
		count= new SpinnerNumberModel(100, 1, 10000, 50);
		JSpinner spinner= new JSpinner(count);
		optionsPanel.add(spinner);
        
		URL url= getClass().getResource("icons/Find24.gif");
		JButton button= new JButton("Submit", new ImageIcon(url));
        optionsPanel.add(button);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doQuery();
			}
		});
        optionsPanel.add(button);
        
		inputVBox.add(optionsPanel, BorderLayout.SOUTH);
		return inputVBox;
	}

	private Vector getCollections(Collection root, Collection collection, Vector collectionsList)
			throws XMLDBException {
		if(!collection.getName().equals(root.getName()))
			collectionsList.add(root.getName());
		String[] childCollections= root.listChildCollections();
		Collection child;
		for (int i= 0; i < childCollections.length; i++) {
			child= root.getChildCollection(childCollections[i]);
			getCollections(child, collection, collectionsList);
		}
		return collectionsList;
	}

	private void open() {
		String workDir = properties.getProperty("working-dir", System.getProperty("user.dir"));
		JFileChooser chooser = new JFileChooser(workDir);
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (chooser.showDialog(this, "Select query file")
			== JFileChooser.APPROVE_OPTION) {
			File selectedDir = chooser.getCurrentDirectory();
			properties.setProperty("working-dir", selectedDir.getAbsolutePath());
			File file = chooser.getSelectedFile();
			if(!file.canRead())
				JOptionPane.showInternalMessageDialog(this, "Cannot read query from file " + file.getAbsolutePath(),
					"Error", JOptionPane.ERROR_MESSAGE);
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = reader.readLine()) != null) {
					buf.append(line);
					buf.append('\n');
				}
				query.setText(buf.toString());
			} catch (FileNotFoundException e) {
				ClientFrame.showErrorMessage(e.getMessage(), e);
			} catch (IOException e) {
				ClientFrame.showErrorMessage(e.getMessage(), e);
			}
		}
	}
	
	private void save() {
		String workDir = properties.getProperty("working-dir", System.getProperty("user.dir"));
		JFileChooser chooser = new JFileChooser(workDir);
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY + JFileChooser.SAVE_DIALOG);
		if (chooser.showDialog(this, "Select file for query export")
			== JFileChooser.APPROVE_OPTION) {
			File selectedDir = chooser.getCurrentDirectory();
			properties.setProperty("working-dir", selectedDir.getAbsolutePath());
			File file = chooser.getSelectedFile();
			if(file.exists() && (!file.canWrite()))
				JOptionPane.showMessageDialog(this, "Can not write query to file " + file.getAbsolutePath(),
						"Error", JOptionPane.ERROR_MESSAGE);
			if(file.exists() &&
				JOptionPane.showConfirmDialog(this, "File exists. Overwrite?", "Overwrite?", 
					JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
				return;
			try {
				FileWriter writer = new FileWriter(file);
				writer.write(query.getText());
				writer.close();
			} catch (FileNotFoundException e) {
				ClientFrame.showErrorMessage(e.getMessage(), e);
			} catch (IOException e) {
				ClientFrame.showErrorMessage(e.getMessage(), e);
			}
		}
	}
	
	private void doQuery() {
		String xpath= (String) query.getText();
		if (xpath.length() == 0)
			return;
		resultDisplay.setText("");
		new QueryThread(xpath).start();
	}
	
	class QueryThread extends Thread {

		private String xpath;
		
		public QueryThread(String query) {
			super();
			this.xpath = query;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			statusMessage.setText("Processing query ...");
			progress.setVisible(true);
			progress.setIndeterminate(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				XPathQueryServiceImpl service= (XPathQueryServiceImpl) collection
				.getService("XPathQueryService", "1.0");
				service.setProperty(OutputKeys.INDENT, properties.getProperty(OutputKeys.INDENT, "yes"));
				ResourceSet result= service.query(xpath);
				
				statusMessage.setText("Retrieving results ...");
				XMLResource resource;
				int howmany= count.getNumber().intValue();
				progress.setIndeterminate(false);
				progress.setMinimum(1);
				progress.setMaximum(howmany);
				int j= 0;
				StringBuffer contents = new StringBuffer();
				for (ResourceIterator i = result.getIterator(); i.hasMoreResources() && j < howmany; j++) {
					resource= (XMLResource) i.nextResource();
					progress.setValue(j);
					try {
						contents.append((String) resource.getContent());
						contents.append("\n");
					} catch (XMLDBException e) {
						ClientFrame.showErrorMessage(
								"An error occurred while retrieving results: "
								+ InteractiveClient.getExceptionMessage(e), e);
					}
				}
				resultDisplay.setText(contents.toString());
				resultDisplay.setCaretPosition(0);
				resultDisplay.scrollToCaret();
				statusMessage.setText("Found " + result.getSize() + " items.");
			} catch (XMLDBException e) {
				ClientFrame.showErrorMessage(
						"An exception occurred during query execution: "
						+ InteractiveClient.getExceptionMessage(e), e);
			}
			if(client.queryHistory.isEmpty() || !((String)client.queryHistory.getLast()).equals(xpath)) {
				client.addToHistory(xpath);
				client.writeQueryHistory();
				if(xpath.length() > 40)
					xpath = xpath.substring(0, 40);
				history.addElement(xpath);
			}
			setCursor(Cursor.getDefaultCursor());
			progress.setVisible(false);
		}
	}
}