/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

import java.lang.Object; 

import java.util.HashMap;

import apidemo.util.NewTabbedPanel.Tab; 

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;


import apidemo.util.HtmlButton;
import apidemo.util.NewTabbedPanel;
import apidemo.util.TCombo;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.VerticalPanel.StackPanel;

import com.ib.client.ScannerSubscription;

import com.ib.controller.Bar;
import com.ib.controller.Instrument;
import com.ib.controller.NewContract;
import com.ib.controller.NewContractDetails;
import com.ib.controller.ScanCode;
import com.ib.controller.ApiController.IDeepMktDataHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.ApiController.IScannerHandler;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DeepSide;
import com.ib.controller.Types.DeepType;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.WhatToShow;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import util.SubscriptionResultsPanel;
import util.VixResultsPanel; 

class OrderByComparator implements Comparator {
        public int compare(Object o1,Object o2) {
                TopModel.MyCustomRow f1 = (TopModel.MyCustomRow)o1;
                TopModel.MyCustomRow f2 = (TopModel.MyCustomRow)o2;
                
                Double d_val1 = f1.m_change; 
                Double d_val2 = f2.m_change;
                
                int i = d_val1.compareTo(d_val2);

                int retStatus=0;
                
                if ( i > 0 ) {
                        retStatus = -1;
                } else if ( i < 0 ) {
                        retStatus = 1;
                } 
                return retStatus;
        }
}

public class MarketDataPanel extends JPanel {
	private final NewContract m_contract = new NewContract();
	private final NewTabbedPanel m_requestPanel = new NewTabbedPanel();
	private  NewTabbedPanel m_resultsPanel = new NewTabbedPanel();
	private TopResultsPanel m_topResultPanel;	

        JButton m_b1 = new JButton("Re-subscribe tabs");     
        JButton m_b2 = new JButton("Print tabs");     

        JButton m_b3 = new JButton("Re-subscribe tabs");     
        
                int secondsPassed = 0;
                Timer myTimer = new Timer();
                TimerTask task = new TimerTask(){
                    public void run(){
                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();
                        System.out.println("Task execution, time is: " + dateFormat.format(date)); 

                        if (ApiDemo.INSTANCE.isConnected() == false)
                        {
                            System.out.println("System is disconnected, reconnecting");
                            while (ApiDemo.INSTANCE.isConnected() == false)
                            {
                                System.out.println("In the loop to try to connect"); 
                                ApiDemo.INSTANCE.connect();
                            }

                            HashMap marketDataHashMap = m_requestPanel.getHashMap(); 
                            Tab topRequestTab = (Tab) marketDataHashMap.get("top-market-data"); 
                            Tab scannerTab = (Tab) marketDataHashMap.get("market-scanner"); 
                            TopRequestPanel topRequestPanel = (TopRequestPanel) topRequestTab.getComponent(); 
                            ScannerRequestPanel scannerRequestPanel = (ScannerRequestPanel) scannerTab.getComponent();

                            scannerRequestPanel.reSubscribeTabs(); 
                            topRequestPanel.reSubscribeTabs();
                            
                        }
                        System.out.println("Inside the TimerTask::run, about to call printTabs()"); 
                        printTabs(); 
                    }
                };
        
	MarketDataPanel() {
           	m_requestPanel.addTab( "top-market-data", new TopRequestPanel() );
/*		m_requestPanel.addTab( "Deep Book", new DeepRequestPanel() );
		m_requestPanel.addTab( "Historical Data", new HistRequestPanel() );
		m_requestPanel.addTab( "Real-time Bars", new RealtimeRequestPanel() ); */ 
		m_requestPanel.addTab( "market-scanner", new ScannerRequestPanel() );
		
		setLayout( new BorderLayout() );
		add( m_requestPanel, BorderLayout.NORTH);
		add( m_resultsPanel);
                
                myTimer.scheduleAtFixedRate(task, 50000, 5800);
                
	}

        public void printTabs()
        {

            HashMap<String,Tab> hashList = m_resultsPanel.getHashMap(); 
            String fileName = "percent-decliners.json";
            FileWriter writer = null;
            String jsonOutput = "";
            TopModel model = null; 
            ScannerResultsPanel childComponent = null; 
            TopResultsPanel childVixComponent = null;
            ArrayList<TopModel.TopRow> rows = null; 
            ArrayList<TopModel.MyCustomRow> myRows = null;
            int totalRows = 0;

            try{
                writer = new FileWriter("C:\\Xampp\\htdocs\\screener\\" + fileName);
            }
            catch(IOException e)
            {
                System.out.println("Inside printTabs, something went wrong in instantiating the file writer");
                e.printStackTrace();
            } 

            jsonOutput += "{"; 

// ******* NASDAQ **********************************************************************


            System.out.println("Inside printTabs,  about to grab STK.NASDAQ");
            Tab nasdaqTab = hashList.get("STK.NASDAQ"); 

            try 
            {
                childComponent = (ScannerResultsPanel)nasdaqTab.getComponent(); 
                model = childComponent.getModel();

                rows = model.getRows(); 
                myRows = new ArrayList(); 

                // first create the array that will be sorted
                for (int i = rows.size() - 1; i >= 0; i--) 
                {
                    TopModel.TopRow row = rows.get(i); 

                    String str_description = row.m_description; 
                    String str_symbol = str_description.replace(" STK SMART", ""); 

                    try
                    {
                        String str_change = row.change(); 
                        str_change = str_change.replace("%", ""); 
                        str_change = str_change.replace("-", ""); 
                        float fl_change = Float.valueOf(str_change); 

                        TopModel.MyCustomRow myRow = new TopModel.MyCustomRow(str_symbol, row.m_last, fl_change, row.m_volume, row.m_low); 
                        myRows.add(myRow);
                    }
                    catch(NullPointerException e) 
                    { 
                        System.out.println("NullPointerException Caught when accessing str_change");
                        e.printStackTrace();
                    } 
                }

                myRows.sort(new OrderByComparator()); 
                totalRows+= myRows.size(); 

                jsonOutput += "\""+ "NASDAQ" + "\":  \n\n { \n"; 
                for (int i = 0; i < myRows.size(); i++) 
                {
                    TopModel.MyCustomRow row = myRows.get(i); 
                    jsonOutput += "\"" + row.m_symbol + "\":{\"last\":" + row.m_last + ",\"change\":" + row.m_change + ",\"volume\":" + row.m_volume + ",\"low\":" + row.m_low + "\n },";
                }
                 jsonOutput += "},";
            }
            catch(NullPointerException e)
            {
                System.out.println("Inside printTabs,  null pointer when grabbing STK.NASDAQ");
                e.printStackTrace();
            } 

// **** NYSE and AMEX *****************************************************************************

            System.out.println("Inside printTabs,  about to grab STK.NYSE");
            Tab nyseTab = hashList.get("STK.NYSE"); 

            try
            {
                childComponent = (ScannerResultsPanel)nyseTab.getComponent(); 
                model = childComponent.getModel();
                rows = model.getRows(); 
                myRows = new ArrayList(); 

                // first create the array that will be sorted
                for (int i = 0; i < rows.size(); i++) 
                {
                    TopModel.TopRow row = rows.get(i); 

                    String str_description = row.m_description; 
                    String str_symbol = str_description.replace(" STK SMART", ""); 

                    try
                    {
                        String str_change = row.change(); 
                        str_change = str_change.replace("%", ""); 
                        str_change = str_change.replace("-", ""); 
                        float fl_change = Float.valueOf(str_change); 

                        TopModel.MyCustomRow myRow = new TopModel.MyCustomRow(str_symbol, row.m_last, fl_change, row.m_volume, row.m_low); 
                        myRows.add(myRow);
                    }
                    catch(NullPointerException e) 
                    { 
                        System.out.println("Inside printTabs (NYSE),  null pointer when grabbing str_change");
                        e.printStackTrace();
                    } 
                }
            }
            catch(NullPointerException e)
            {
                System.out.println("Inside printTabs,  null pointer when grabbing STK.NYSE");
                e.printStackTrace();
            } 

            System.out.println("Inside printTabs,  about to grab STK.AMEX");
            Tab amexTab = hashList.get("STK.AMEX"); 

            try
            {
                childComponent = (ScannerResultsPanel)amexTab.getComponent(); 
                model = childComponent.getModel();
                rows = model.getRows(); 

                for (int i = 0; i < rows.size(); i++) 
                {
                    TopModel.TopRow row = rows.get(i); 

                    String str_description = row.m_description; 
                    String str_symbol = str_description.replace(" STK SMART", ""); 

                    try
                    {
                        String str_change = row.change(); 
                        str_change = str_change.replace("%", ""); 
                        str_change = str_change.replace("-", ""); 
                        float fl_change = Float.valueOf(str_change); 

                        TopModel.MyCustomRow myRow = new TopModel.MyCustomRow(str_symbol, row.m_last, fl_change, row.m_volume, row.m_low); 
                        myRows.add(myRow);
                    }
                    catch(NullPointerException e) 
                    { 
                        System.out.println("Inside printTabs (AMEX),  null pointer when grabbing str_change");
                        e.printStackTrace();
                    } 
                }

                if ((amexTab != null) || (nyseTab != null))
                {
                    myRows.sort(new OrderByComparator()); 
                    totalRows+= myRows.size(); 

                    jsonOutput += "\""+ "NYSEAMEX" + "\":  \n\n { \n"; 
                    for (int i = 0; i < myRows.size(); i++) 
                    {
                        TopModel.MyCustomRow row = myRows.get(i); 
                        jsonOutput += "\"" + row.m_symbol + "\":{\"last\":" + row.m_last + ",\"change\":" + row.m_change + ",\"volume\":" + row.m_volume + ",\"low\":" + row.m_low + "\n },";
                    }
                    jsonOutput += "},";
                }
            }
            catch(NullPointerException e)
            {
                System.out.println("Inside printTabs,  null pointer when grabbing STK.AMEX");
                e.printStackTrace();
            } 

// ***** PINK *************************************************************************

            System.out.println("Inside printTabs,  about to grab STK.PINK");
            Tab pinkTab = hashList.get("STK.PINK"); 

            try
            {
                childComponent = (ScannerResultsPanel)pinkTab.getComponent(); 
                model = childComponent.getModel();
                rows = model.getRows(); 
                myRows = new ArrayList(); 

                for (int i = 0; i < rows.size(); i++) 
                {
                    TopModel.TopRow row = rows.get(i); 

                    String str_description = row.m_description; 
                    String str_symbol = str_description.replace(" STK SMART", "");

                    try
                    {
                        String str_change = row.change(); 
                        str_change = str_change.replace("%", ""); 
                        str_change = str_change.replace("-", ""); 
                        float fl_change = Float.valueOf(str_change); 

                        TopModel.MyCustomRow myRow = new TopModel.MyCustomRow(str_symbol, row.m_last, fl_change, row.m_volume, row.m_low); 
                        myRows.add(myRow);
                    }
                    catch(NullPointerException e) 
                    { 
                        System.out.println("Inside printTabs (PINK),  null pointer when grabbing str_change");
                        e.printStackTrace();
                    } 
                }

                myRows.sort(new OrderByComparator()); 
                totalRows+= myRows.size(); 

                jsonOutput += "\""+ "PINK" + "\":  \n\n { \n"; 
                for (int i = 0; i < myRows.size(); i++) 
                {
                    TopModel.MyCustomRow row = myRows.get(i); 
                    jsonOutput += "\"" + row.m_symbol + "\":{\"last\":" + row.m_last + ",\"change\":" + row.m_change + ",\"volume\":" + row.m_volume + ",\"low\":" + row.m_low + "\n },";
                }
                jsonOutput += "}";
            }
            catch(NullPointerException e)
            {
                System.out.println("Inside printTabs,  null pointer when grabbing STK.PINK");
                e.printStackTrace();
            }                     

// ******* VIX **********************************************************************

            System.out.println("Inside printTabs,  about to grab the VIX tab");
            Tab vixTab = hashList.get("VIX"); 

            try 
            {
                System.out.println("Successul in grabbing the VIX tab"); 
                childVixComponent = (TopResultsPanel)vixTab.getComponent(); 
                model = childVixComponent.getModel();

                rows = model.getRows(); 
                myRows = new ArrayList(); 

                System.out.println("About to go throught the VIX rows"); 

                // first create the array that will be sorted
                for (int i = rows.size() - 1; i >= 0; i--) 
                {
                    TopModel.TopRow row = rows.get(i); 
                    String str_description = row.m_description.trim(); 

                    if (str_description.equals("VIX IND CBOE"))
                    {
                        try
                        {
                            String str_vixLast = Double.toString(row.m_last);
                            System.out.println("Found the VIX row, it's last value is " + str_vixLast);
                            jsonOutput += ",\"VIX\": \n"; 
                            jsonOutput += str_vixLast + "\n";
                        }
                        catch(NullPointerException e) 
                        { 
                            System.out.println("NullPointerException Caught when accessing VIX's last value");
                            e.printStackTrace();
                        } 
                    }
                    else
                    {
                        System.out.println("str_description != VIX IND CBOE");
                    }
                }
            }
            catch(NullPointerException e)
            {
                System.out.println("Inside printTabs, null pointer when grabbing VIX");
                e.printStackTrace();
            } 

            jsonOutput += "}"; 
            jsonOutput = jsonOutput.replace("},}", "}}");
            // need this a second time 
            jsonOutput = jsonOutput.replace("},}", "}}");

            try{
                writer.append(jsonOutput);
                writer.flush();
                writer.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            } 

            if  (totalRows > 150)
            {
                HashMap marketDataHashMap = m_requestPanel.getHashMap(); 
                ScannerRequestPanel scannerRequestPanel = (ScannerRequestPanel) marketDataHashMap.get("market-scanner"); 
                scannerRequestPanel.reSubscribeTabs(); 
            }
        }

	private class DeepRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		
		DeepRequestPanel() {
			HtmlButton reqDeep = new HtmlButton( "Request Deep Market Data") {
				@Override protected void actionPerformed() {
					onDeep();
				}
			};
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( reqDeep);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( butPanel);
		}

		protected void onDeep() {
			m_contractPanel.onOK();
			DeepResultsPanel resultPanel = new DeepResultsPanel();
			m_resultsPanel.addTab( "Deep " + m_contract.symbol(), resultPanel, true, true);
			ApiDemo.INSTANCE.controller().reqDeepMktData(m_contract, 6, resultPanel);
		}
	}

	private static class DeepResultsPanel extends NewTabPanel implements IDeepMktDataHandler {
		final DeepModel m_buy = new DeepModel();
		final DeepModel m_sell = new DeepModel();

		DeepResultsPanel() {
			HtmlButton desub = new HtmlButton( "Desubscribe") {
				public void actionPerformed() {
					onDesub();
				}
			};
			
			JTable buyTab = new JTable( m_buy);
			JTable sellTab = new JTable( m_sell);
			
			JScrollPane buyScroll = new JScrollPane( buyTab);
			JScrollPane sellScroll = new JScrollPane( sellTab);
			
			JPanel mid = new JPanel( new GridLayout( 1, 2) );
			mid.add( buyScroll);
			mid.add( sellScroll);
			
			setLayout( new BorderLayout() );
			add( mid);
			add( desub, BorderLayout.SOUTH);
		}
		
		protected void onDesub() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}

		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}
		
		@Override public void updateMktDepth(int pos, String mm, DeepType operation, DeepSide side, double price, int size) {
			if (side == DeepSide.BUY) {
				m_buy.updateMktDepth(pos, mm, operation, price, size);
			}
			else {
				m_sell.updateMktDepth(pos, mm, operation, price, size);
			}
		}

		class DeepModel extends AbstractTableModel {
			final ArrayList<DeepRow> m_rows = new ArrayList<DeepRow>();

			@Override public int getRowCount() {
				return m_rows.size();
			}

			public void updateMktDepth(int pos, String mm, DeepType operation, double price, int size) {
				switch( operation) {
					case INSERT:
						m_rows.add( pos, new DeepRow( mm, price, size) );
						fireTableRowsInserted(pos, pos);
						break;
					case UPDATE:
						m_rows.get( pos).update( mm, price, size);
						fireTableRowsUpdated(pos, pos);
						break;
					case DELETE:
						if (pos < m_rows.size() ) {
							m_rows.remove( pos);
						}
						else {
							// this happens but seems to be harmless
							// System.out.println( "can't remove " + pos);
						}
						fireTableRowsDeleted(pos, pos);
						break;
				}
			}

			@Override public int getColumnCount() {
				return 3;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Mkt Maker";
					case 1: return "Price";
					case 2: return "Size";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				DeepRow row = m_rows.get( rowIn);
				
				switch( col) {
					case 0: return row.m_mm;
					case 1: return row.m_price;
					case 2: return row.m_size;
					default: return null;
				}
			}
		}
		
		static class DeepRow {
			String m_mm;
			double m_price;
			int m_size;

			public DeepRow(String mm, double price, int size) {
				update( mm, price, size);
			}
			
			void update( String mm, double price, int size) {
				m_mm = mm;
				m_price = price;
				m_size = size;
			}
		}
	}

	private class HistRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final UpperField m_end = new UpperField();
		final UpperField m_duration = new UpperField();
		final TCombo<DurationUnit> m_durationUnit = new TCombo<DurationUnit>( DurationUnit.values() );
		final TCombo<BarSize> m_barSize = new TCombo<BarSize>( BarSize.values() );
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		HistRequestPanel() { 		
			m_end.setText( "20120101 12:00:00");
			m_duration.setText( "1");
			m_durationUnit.setSelectedItem( DurationUnit.WEEK);
			m_barSize.setSelectedItem( BarSize._1_hour);
			
			HtmlButton button = new HtmlButton( "Request historical data") {
				@Override protected void actionPerformed() {
					onHistorical();
				}
			};
			
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "End", m_end);
			paramPanel.add( "Duration", m_duration);
			paramPanel.add( "Duration unit", m_durationUnit);
			paramPanel.add( "Bar size", m_barSize);
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onHistorical() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( true);
			ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, m_end.getText(), m_duration.getInt(), m_durationUnit.getSelectedItem(), m_barSize.getSelectedItem(), m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_resultsPanel.addTab( "Historical " + m_contract.symbol(), panel, true, true);
		}
	}

	private class RealtimeRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		RealtimeRequestPanel() { 		
			HtmlButton button = new HtmlButton( "Request real-time bars") {
				@Override protected void actionPerformed() {
					onRealTime();
				}
			};
	
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onRealTime() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( false);
			ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract, m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_resultsPanel.addTab( "Real-time " + m_contract.symbol(), panel, true, true);
		}
	}
	
	static class BarResultsPanel extends NewTabPanel implements IHistoricalDataHandler, IRealTimeBarHandler {
		final BarModel m_model = new BarModel();
		final ArrayList<Bar> m_rows = new ArrayList<Bar>();
		final boolean m_historical;
		final Chart m_chart = new Chart( m_rows);
		
		BarResultsPanel( boolean historical) {
			m_historical = historical;
			
			JTable tab = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( tab) {
				public Dimension getPreferredSize() {
					Dimension d = super.getPreferredSize();
					d.width = 500;
					return d;
				}
			};

			JScrollPane chartScroll = new JScrollPane( m_chart);

			setLayout( new BorderLayout() );
			add( scroll, BorderLayout.WEST);
			add( chartScroll, BorderLayout.CENTER);
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			if (m_historical) {
				ApiDemo.INSTANCE.controller().cancelHistoricalData( this);
			}
			else {
				ApiDemo.INSTANCE.controller().cancelRealtimeBars( this);
			}
		}

		@Override public void historicalData(Bar bar, boolean hasGaps) {
			m_rows.add( bar);
		}
		
		@Override public void historicalDataEnd() {
			fire();
		}

		@Override public void realtimeBar(Bar bar) {
			m_rows.add( bar); 
			fire();
		}
		
		private void fire() {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					m_model.fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
					m_chart.repaint();
				}
			});
		}

		class BarModel extends AbstractTableModel {
			@Override public int getRowCount() {
				return m_rows.size();
			}

			@Override public int getColumnCount() {
				return 7;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Date/time";
					case 1: return "Open";
					case 2: return "High";
					case 3: return "Low";
					case 4: return "Close";
					case 5: return "Volume";
					case 6: return "WAP";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				Bar row = m_rows.get( rowIn);
				switch( col) {
					case 0: return row.formattedTime();
					case 1: return row.open();
					case 2: return row.high();
					case 3: return row.low();
					case 4: return row.close();
					case 5: return row.volume();
					case 6: return row.wap();
					default: return null;
				}
			}
		}		
	}
	
	
	private class TopRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
                Map<String, NewContract> m_newContractHash = new HashMap<String, NewContract>();
                
		TopRequestPanel() {
			HtmlButton reqTop = new HtmlButton( "Request Top Market Data") {
				@Override protected void actionPerformed() {
					onTop();
				}
			};
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( reqTop);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);

/*                        
                        m_contractPanel.add( "Re-subscribe tabs", m_b3); 
                        m_b3.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                System.out.println("Testing button.");
                            }
                        });
  */
                        
			add( Box.createHorizontalStrut(20));
			add( butPanel);
		}

		protected void onTop() {
			m_contractPanel.onOK();

                        UpperField symbol = m_contractPanel.getSymbol(); 
                        String str_symbol = symbol.getString();
                        System.out.println("The symbol is " + symbol.getString()); 
                        
			if (m_topResultPanel == null) {
				m_topResultPanel = new TopResultsPanel();
				m_resultsPanel.addTab( str_symbol, m_topResultPanel, true, true);
			}
			m_topResultPanel.m_model.addRow( m_contract);
                        m_newContractHash.put(m_contract.getSymbol(), m_contract ); 
		}
                
                public void reSubscribeTabs(){
                    m_topResultPanel.m_model.removeAllRows(); 
                    
                    Iterator it = m_newContractHash.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            NewContract myNewContract = (NewContract) pair.getValue();
                            m_topResultPanel.m_model.addRow( m_contract);
                        }
                    
                }

	}
	
	public class TopResultsPanel extends NewTabPanel {
		final TopModel m_model = new TopModel();
		final JTable m_tab = new TopTable( m_model);
		final TCombo<MktDataType> m_typeCombo = new TCombo<MktDataType>( MktDataType.values() );

		TopResultsPanel() {
			m_typeCombo.removeItemAt( 0);

			JScrollPane scroll = new JScrollPane( m_tab);

			HtmlButton reqType = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onReqType();
				}
			};

			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( "Market data type", m_typeCombo, reqType);
			
			setLayout( new BorderLayout() );
			add( scroll);
			add( butPanel, BorderLayout.SOUTH);
		}
		
		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			m_model.desubscribe();
			m_topResultPanel = null;
		}
                
                public TopModel getModel(){
                    return m_model; 
                }

		void onReqType() {
			ApiDemo.INSTANCE.controller().reqMktDataType( m_typeCombo.getSelectedItem() );
		}
		
		class TopTable extends JTable {
			public TopTable(TopModel model) { super( model); }

			@Override public TableCellRenderer getCellRenderer(int rowIn, int column) {
				TableCellRenderer rend = super.getCellRenderer(rowIn, column);
				m_model.color( rend, rowIn, getForeground() );
				return rend;
			}
		}
	}		

	private class ScannerRequestPanel extends JPanel {
		final UpperField m_numRows = new UpperField( "50");
		final TCombo<ScanCode> m_scanCode = new TCombo<ScanCode>( ScanCode.values() );
		final TCombo<Instrument> m_instrument = new TCombo<Instrument>( Instrument.values() );
		final UpperField m_location = new UpperField( "STK.PINK", 9);
		final TCombo<String> m_stockType = new TCombo<String>( "ALL", "STOCK", "ETF");
                Map<String, SubscriptionResultsPanel> m_subscriptionResultsHash = new HashMap<String, SubscriptionResultsPanel>();

		ScannerRequestPanel() {
			HtmlButton go = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onGo();
				}
			};
			
			VerticalPanel paramsPanel = new VerticalPanel();
			paramsPanel.add( "Scan code", m_scanCode);
			paramsPanel.add( "Instrument", m_instrument);
			paramsPanel.add( "Location", m_location, Box.createHorizontalStrut(10), go);
			paramsPanel.add( "Stock type", m_stockType);
			paramsPanel.add( "Num rows", m_numRows);

                        setLayout( new BorderLayout() );
			add( paramsPanel, BorderLayout.NORTH);
                        
                        paramsPanel.add( "Re-subscribe tabs", m_b1); 
                        paramsPanel.add( "Print tabs", m_b2); 

                        m_b1.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                reSubscribeTabs(); 
                                HashMap marketDataHashMap = m_requestPanel.getHashMap(); 
                                Tab topRequestTab = (Tab) marketDataHashMap.get("top-market-data"); 
                                TopRequestPanel topRequestPanel = (TopRequestPanel) topRequestTab.getComponent(); 
                                topRequestPanel.reSubscribeTabs();
                            }
                        });
                  
                        m_b2.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                printTabs(); 
                            }
                        });

		}

		protected void onGo() {
			ScannerSubscription sub = new ScannerSubscription();
                        int reqId;
                        
			sub.numberOfRows( m_numRows.getInt() );
			sub.scanCode( m_scanCode.getSelectedItem().toString() );
			sub.instrument( m_instrument.getSelectedItem().toString() );
			sub.locationCode( m_location.getText() );
			sub.stockTypeFilter( m_stockType.getSelectedItem().toString() );
                        sub.belowPrice(11.00);
                        sub.abovePrice(0.0015);
                        sub.aboveVolume(10000);
			
			ScannerResultsPanel resultsPanel = new ScannerResultsPanel();

			m_resultsPanel.addTab(m_location.getText(), resultsPanel, true, true);

			reqId = ApiDemo.INSTANCE.controller().reqScannerSubscription( sub, resultsPanel);

                        SubscriptionResultsPanel subscriptionResultsPanel = new SubscriptionResultsPanel(reqId, sub, resultsPanel); 
                        m_subscriptionResultsHash.put(m_location.getText(), subscriptionResultsPanel);
		}

                public void reSubscribeTabs(){
                    HashMap<String,Tab> hashList = m_resultsPanel.getHashMap();

                    Iterator it = m_subscriptionResultsHash.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            SubscriptionResultsPanel subscriptionResultsPanel = (SubscriptionResultsPanel) pair.getValue();
                            ScannerSubscription sub = subscriptionResultsPanel.m_sub;
                            ScannerResultsPanel resultsPanel = subscriptionResultsPanel.m_resultsPanel;
                            
                            String str_locationCode = (String)pair.getKey(); 

                            System.out.println(pair.getKey() + " = " + subscriptionResultsPanel.m_sub.locationCode());
                            
                            ApiDemo.INSTANCE.controller().cancelScannerSubscription(subscriptionResultsPanel.m_reqId);

                            Tab currentTab = hashList.get(str_locationCode);
                            ScannerResultsPanel scannerResultsPanel = (ScannerResultsPanel)currentTab.getComponent();
                            scannerResultsPanel.m_conids.clear();
                            TopModel model = scannerResultsPanel.getModel();
                            model.removeAllRows(); 
                            
                            ApiDemo.INSTANCE.controller().reReqScannerSubscription(subscriptionResultsPanel.m_reqId, sub, resultsPanel);
                    }
                }

	}

	public static class ScannerResultsPanel extends NewTabPanel implements IScannerHandler {
		final HashSet<Integer> m_conids = new HashSet<Integer>();
		public TopModel m_model = new TopModel();

		ScannerResultsPanel() {
			JTable table = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( table);
			setLayout( new BorderLayout() );
			add(scroll);
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelScannerSubscription( this);
			m_model.desubscribe();
		}

		public void cancelSubscription() {
			ApiDemo.INSTANCE.controller().cancelScannerSubscription(this);
			m_model.desubscribe();
		}
                
                public TopModel getModel(){
                    return m_model; 
                }
                
		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		@Override public void scannerParameters(String xml) {
			try {
				File file = File.createTempFile( "pre", ".xml");
				FileWriter writer = new FileWriter( file);
				writer.write( xml);
				writer.close();

				Desktop.getDesktop().open( file);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override public void scannerData(int rank, final NewContractDetails contractDetails, String legsStr) {
			if (!m_conids.contains( contractDetails.conid() ) ) {
				m_conids.add( contractDetails.conid() );
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						m_model.addRow( contractDetails.contract() );
					}
				});
			}
		}

		@Override public void scannerDataEnd() {
			// we could sort here
		}
	}
}
