/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;


import java.awt.BorderLayout; 
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;

// the following are for pasting the order from the clipboard 

import java.awt.HeadlessException; 
import java.awt.Toolkit; 
import java.awt.datatransfer.Clipboard; 
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane; 
import javax.swing.JCheckBox;

import apidemo.util.HtmlButton;
import apidemo.util.NewLookAndFeel;
import apidemo.util.NewTabbedPanel;
import apidemo.util.VerticalPanel;

// I pasted these in for order handling 
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.TimeInForce;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.OrderType;
import com.ib.controller.ApiController.ILiveOrderHandler;

import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IBulletinHandler;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ITimeHandler;
import com.ib.controller.Formats;
import com.ib.controller.Types.NewsType;

public class ApiDemo implements IConnectionHandler {
	static { NewLookAndFeel.register(); }
	static ApiDemo INSTANCE = new ApiDemo();

	private final JTextArea m_inLog = new JTextArea();
	private final JTextArea m_outLog = new JTextArea();
	private final Logger m_inLogger = new Logger( m_inLog);
	private final Logger m_outLogger = new Logger( m_outLog);
	private final ApiController m_controller = new ApiController( this, m_inLogger, m_outLogger);
	private final ArrayList<String> m_acctList = new ArrayList<String>();
	private final JFrame m_frame = new JFrame();
	private final NewTabbedPanel m_tabbedPanel = new NewTabbedPanel(true);
	private final ConnectionPanel m_connectionPanel = new ConnectionPanel();
	private final MarketDataPanel m_mktDataPanel = new MarketDataPanel();
	private final ContractInfoPanel m_contractInfoPanel = new ContractInfoPanel();
	private final TradingPanel m_tradingPanel = new TradingPanel();
	private final AccountInfoPanel m_acctInfoPanel = new AccountInfoPanel();
	private final OptionsPanel m_optionsPanel = new OptionsPanel();
	private final AdvisorPanel m_advisorPanel = new AdvisorPanel();
	private final ComboPanel m_comboPanel = new ComboPanel();
	private final StratPanel m_stratPanel = new StratPanel();
	private final JTextArea m_msg = new JTextArea();

	// getter methods
	public ArrayList<String> accountList() 	{ return m_acctList; }
	public ApiController controller() 		{ return m_controller; }
	public JFrame frame() 					{ return m_frame; }
        
        public boolean isConnected(){
            String str_status = m_connectionPanel.m_status.getText(); 
            System.out.println("Inside isConnected, str_status is " + str_status); 
                    
            if (str_status == "connected")
            {
                System.out.println("We are connected"); 
                return true;
            }
            else
            {
                System.out.println("We are NOT connected"); 
                return false; 
            }
        }
        
        public void connect(){
            System.out.println("Inside connect"); 
            m_controller.connect( "127.0.0.1", 7496, 0);
        }


	public static void main(String[] args) {
		INSTANCE.run();
	}
	
	private void run() {
		m_tabbedPanel.addTab( "Connection", m_connectionPanel);
		m_tabbedPanel.addTab( "Market Data", m_mktDataPanel);
		m_tabbedPanel.addTab( "Trading", m_tradingPanel);
		m_tabbedPanel.addTab( "Account Info", m_acctInfoPanel);
//		m_tabbedPanel.addTab( "Options", m_optionsPanel);
//		m_tabbedPanel.addTab( "Combos", m_comboPanel);
		m_tabbedPanel.addTab( "Contract Info", m_contractInfoPanel);
//		m_tabbedPanel.addTab( "Advisor", m_advisorPanel);
		// m_tabbedPanel.addTab( "Strategy", m_stratPanel); in progress
			
		m_msg.setEditable( false);
		m_msg.setLineWrap( true);
		JScrollPane msgScroll = new JScrollPane( m_msg);
		msgScroll.setPreferredSize( new Dimension( 10000, 120) );

		JScrollPane outLogScroll = new JScrollPane( m_outLog);
		outLogScroll.setPreferredSize( new Dimension( 10000, 120) );

		JScrollPane inLogScroll = new JScrollPane( m_inLog);
		inLogScroll.setPreferredSize( new Dimension( 10000, 120) );

		NewTabbedPanel bot = new NewTabbedPanel();
		bot.addTab( "Messages", msgScroll);
		bot.addTab( "Log (out)", outLogScroll);
		bot.addTab( "Log (in)", inLogScroll);
		
        m_frame.add( m_tabbedPanel);
        m_frame.add( bot, BorderLayout.SOUTH);
        m_frame.setSize( 1024, 768);
        m_frame.setVisible( true);
        m_frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
        
        // make initial connection to local host, port 7496, client id 0
		m_controller.connect( "127.0.0.1", 7496, 0);
    }
	
	@Override public void connected() {
		show( "connected");
		m_connectionPanel.m_status.setText( "connected");
		
                
                
		m_controller.reqCurrentTime( new ITimeHandler() {
			@Override public void currentTime(long time) {
				show( "Server date/time is " + Formats.fmtDate(time * 1000) );
			}
		});
		
		m_controller.reqBulletins( true, new IBulletinHandler() {
			@Override public void bulletin(int msgId, NewsType newsType, String message, String exchange) {
				String str = String.format( "Received bulletin:  type=%s  exchange=%s", newsType, exchange);
				show( str);
				show( message);
			}
		});
	}
	
	@Override public void disconnected() {
		show( "disconnected");
		m_connectionPanel.m_status.setText( "disconnected");
	}

	@Override public void accountList(ArrayList<String> list) {
		show( "Received account list");
		m_acctList.clear();
		m_acctList.addAll( list);
	}

	@Override public void show( final String str) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				m_msg.append(str);
				m_msg.append( "\n\n");
				
				Dimension d = m_msg.getSize();
				m_msg.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
			}
		});
	}

	@Override public void error(Exception e) {
		show( e.toString() );
	}
	
	@Override public void message(int id, int errorCode, String errorMsg) {
		show( id + " " + errorCode + " " + errorMsg);
	}
        

	
	private class ConnectionPanel extends JPanel {
		private final JTextField m_host = new JTextField(7);
		private final JTextField m_port = new JTextField( "7496", 7);
		private final JTextField m_clientId = new JTextField("0", 7);
                
                // brent 
		public final JTextField m_orderText = new JTextField(25);
                JButton m_b1 = new JButton("Send Order");     
                JButton m_b35 = new JButton("3.5%");     
                JButton m_b2 = new JButton("4.2%"); 
                JButton m_b3 = new JButton("5%"); 
                JButton m_b515 = new JButton("5.15%"); 
                JButton m_b55 = new JButton("5.5%"); 
                JButton m_b4 = new JButton("6%");                 
                JButton m_b5 = new JButton("7%");                 
                JButton m_b6 = new JButton("8%");
                JButton m_b7 = new JButton("9%");                 
                JButton m_b8 = new JButton("10%");                 
                JButton m_b9 = new JButton("11%");                 
                JButton m_b10 = new JButton("12%");                 
                JButton m_b11 = new JButton("13%");
                JCheckBox m_bracketOrderOnly = new JCheckBox("Bracket only"); 
		
                private final JLabel m_status = new JLabel("Disconnected");
		
                public void sendOrder(double percentageProfit)
                {
                    System.out.println("inside sendOrder"); 
                }
                
		public ConnectionPanel() {
			HtmlButton connect = new HtmlButton("Connect") {
				@Override public void actionPerformed() {
					onConnect();
				}
			};

			HtmlButton disconnect = new HtmlButton("Disconnect") {
				@Override public void actionPerformed() {
					m_controller.disconnect();
				}
			};

			JPanel p1 = new JPanel();
/*			p1.add( "Host", m_host);
			p1.add( "Port", m_port);
			p1.add( "Client ID", m_clientId); */

// the order text box 
//                        p1.add( "Order:", m_orderText);
			
                        


                        m_b1.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                    
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 9.00*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;
  
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } // end of the click event handler for 
                        });                    


                        // Take fast 3.5% 
                        m_b35.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                
                                
                                NewOrder o = new NewOrder();
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);



System.out.println("About to check for checkbox value");           

if (m_bracketOrderOnly.isSelected()  )
{
    System.out.println("Selected");
}else
{
    System.out.println("Not Selected");
}

/* 
System.exit(0); 
*/
            
                                
                                
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 

                                double fl_childSellPrice = fl_price + 0.035*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 3.5%" 

                        
                        // Take fast 4.2% 
                        m_b2.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.042*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 4.2%" 

                        // Take fast 5% 
                        m_b3.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.05*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 5%" 
                        
                        
                        // Take fast 5.15% 
                        m_b515.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.0515*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 5.15%" 
                        
                        

                        // Take fast 5.5% 
                        m_b55.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.055*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 5.5%" 

                        // Take fast 6% 
                        m_b4.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.06*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 6%" 
                        
                        // Take fast 7% 
                        m_b5.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.07*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 7%"                    
                        
                        // Take fast 8% 
                        m_b6.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.08*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 8%"  
                        
                        // Take fast 9% 
                        m_b7.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.09*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 9%"  
                        
                        // Take fast 10% 
                        m_b8.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.10*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 10%"  
                        
                        // Take fast 11% 
                        m_b9.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.11*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 11%"  
                        
                        // Take fast 12% 
                        m_b10.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.12*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 12%"  
                        
                        // Take fast 13% 
                        m_b11.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // grab the pasted order from the text box 
                                String str_orderText = ""; 
                                try
                                {
                                    str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                                    System.out.println(""); 
                                    System.out.println(str_orderText); 
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                
                                /*
                                String str_orderText = m_orderText.getText().trim();
                                
                                System.out.println(""); 
                                System.out.println(str_orderText);   */

                                if (str_orderText.isEmpty())
                                {
                                    JOptionPane.showMessageDialog(p1, "The order string is empty");
                                    return; 
                                }

                                String[] arr_orderParameters = str_orderText.split(" ");
                                String str_symbol = arr_orderParameters[0];
                                str_symbol = str_symbol.replaceAll("\\.", " ");
                                
                                
                                System.out.println("Symbol is " + str_symbol);
                                
                                String str_numShares = arr_orderParameters[2].replaceAll(",", "");
                                int i_numShares = Integer.parseInt(str_numShares);
                                if (i_numShares > 500000)
                                {
                                    i_numShares = 500000; 
                                }
                                System.out.println("Number of shares is " + i_numShares);

                                String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                                double fl_price = Double.parseDouble(str_price); 
                                System.out.println("Parent sell price is " + fl_price);
                                
                                String str_percentage = arr_orderParameters[4].replaceAll("\\(", "");
                                str_percentage = str_percentage.replaceAll("\\)", "");
                                str_percentage = str_percentage.replaceAll("\\%", "");
                                double fl_percentage = Double.parseDouble(str_percentage); 
                                System.out.println("Percentage is " + fl_percentage);

                                if (fl_percentage < 0)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nPlacing buy stop order " + str_percentage, "Buy Stop Order", JOptionPane.NO_OPTION);
                                }
                                else if (fl_percentage < 12)
                                {
                                  javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nNOT placed, percentage is only " + str_percentage, "Order NOT Placed", JOptionPane.NO_OPTION);
                                  return;
                                }

                                // Order starts here                                 
                                NewContract myContract = new NewContract();
                                myContract.symbol(str_symbol); 
                                myContract.secType(SecType.STK);
                                myContract.exchange("SMART"); 
                                myContract.primaryExch("ISLAND");
                                myContract.currency("USD"); 
                                NewOrder o = new NewOrder();

                                
// reqMktData

                                
                                o.account("U1203596"); 
                                o.action(Action.BUY);

                                if (fl_percentage < 0)
                                {
                                    o.orderType(OrderType.STP); 
                                }
                                else
                                {
                                    o.orderType(OrderType.LMT);                                     
                                }

                                o.lmtPrice(fl_price);
                                o.totalQuantity(i_numShares);
                                o.tif(TimeInForce.DAY);
                                o.outsideRth(true);

                                // grab the latest (max) order id
                                int i_parentBuyOrderId = 0; 
                                int i_childSellOrderId = 0;
                                int i_childStopOrderId = 0;
                                int i_nextOrderId = 0; 
                                try
                                {
                                    // new input stream created
                                    FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                                    BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                                    String str_latestOrderId = fisReader.readLine();
                                    i_parentBuyOrderId = Integer.parseInt(str_latestOrderId);

                                    System.out.println("The latest order id is " + i_parentBuyOrderId);
                                    fis.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                o.orderId(i_parentBuyOrderId); 
                                o.transmit(true);

                                System.out.println("The NEXT parent buy order id is " + i_parentBuyOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                                System.out.println("You have just sent off the parent order");

                                // Limit sell stop order

                                i_childStopOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oStop = new NewOrder();
                                oStop.action(Action.SELL);
                                oStop.orderType(OrderType.STP); 
                                double fl_childStopPrice = fl_price - 0.80*fl_price;
                                if (fl_childStopPrice > 1.00)
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                                }
                                else 
                                {
                                    fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                                }  
                                System.out.println("Child stop order price is " + fl_childStopPrice);
                                oStop.auxPrice(fl_childStopPrice);
                                oStop.totalQuantity(i_numShares);
                                oStop.tif(TimeInForce.DAY);
                                oStop.outsideRth(true);
                                oStop.orderId(i_childStopOrderId);
                                oStop.parentId(i_parentBuyOrderId); 
                                oStop.transmit(true);

                                System.out.println("The NEXT child stop order id is " + i_childStopOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                                System.out.println("You have just sent off the child stop order");

                                // Limit sell bracket order

                                i_childSellOrderId = i_parentBuyOrderId + 2; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                double fl_childSellPrice = fl_price + 0.13*fl_price;
                                if (fl_childSellPrice > 1.00)
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                }
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }  
                                System.out.println("Child sell price is " + fl_childSellPrice);
                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                System.out.println("The NEXT child sell order id is " + i_childSellOrderId);
                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                i_nextOrderId = i_parentBuyOrderId + 3;

                                
                                // now we write the next order ID to the output file
                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    // convert next orderId to string 
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("");
                                    sb.append(i_nextOrderId);
                                    String str_nextOrderId = sb.toString();
                                    byte[] arr_nextOrderId = str_nextOrderId.getBytes();
                                    
                                    // byte[] arr_outputString = 
                                    fos.write(arr_nextOrderId);
                                    fos.flush();
                                    fos.close();
                                }
                                catch(Exception e)
                                {
                                    // if any I/O error occurs
                                    System.out.println(e.getMessage()); 
                                    e.printStackTrace();
                                }

                                // Clear the order text box
                                m_orderText.setText(""); 

                                javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                                
                	    } 
                        });  // end of the click event handler for "Fast 13%"  

                        
                        p1.add("Send:", m_b1);
                        p1.add("Send:", m_b35); 
                        p1.add("Send:", m_b2); 
                        p1.add("Send:", m_b3); 
                        p1.add("Send:", m_b515);
                        p1.add("Send:", m_b55); 
                        p1.add("Send:", m_b4); 
                        p1.add("Send:", m_b5); 
                        p1.add("Send:", m_b6); 
                        p1.add("Send:", m_b7); 
                        p1.add("Send:", m_b8); 
                        p1.add("Send:", m_b9); 
                        p1.add("Send:", m_b10); 
                        p1.add("Send:", m_b11);
                        p1.add("", m_bracketOrderOnly); 

			JPanel p2 = new VerticalPanel();
                        // p2.add(m_b2); 
			p2.add( connect);
			p2.add( disconnect);
			p2.add( Box.createVerticalStrut(20));

			JPanel p3 = new VerticalPanel();
			p3.setBorder( new EmptyBorder( 20, 0, 0, 0));
			p3.add( "Connection status: ", m_status);
			
			JPanel p4 = new JPanel( new BorderLayout() );
			p4.add( p1, BorderLayout.WEST);
			p4.add( p2);
			p4.add( p3, BorderLayout.SOUTH);

			setLayout( new BorderLayout() );
			add( p4, BorderLayout.NORTH);
		}

		protected void onConnect() {
			int port = Integer.parseInt( m_port.getText() );
			int clientId = Integer.parseInt( m_clientId.getText() );
			m_controller.connect( m_host.getText(), port, clientId);
		}
	}
	
	private static class Logger implements ILogger {
		final private JTextArea m_area;

		Logger( JTextArea area) {
			m_area = area;
		}

		@Override public void log(final String str) {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
//					m_area.append(str);
//					
//					Dimension d = m_area.getSize();
//					m_area.scrollRectToVisible( new Rectangle( 0, d.height, 1, 1) );
				}
			});
		}
	}
}

// do clearing support
// change from "New" to something else
// more dn work, e.g. deltaNeutralValidation
// add a "newAPI" signature
// probably should not send F..A position updates to listeners, at least not to API; also probably not send FX positions; or maybe we should support that?; filter out models or include it 
// finish or remove strat panel
// check all ps
// must allow normal summary and ledger at the same time
// you could create an enum for normal account events and pass segment as a separate field
// check pacing violation
// newticktype should break into price, size, and string?
// give "already subscribed" message if appropriate

// BUGS
// When API sends multiple snapshot requests, TWS sends error "Snapshot exceeds 100/sec" even when it doesn't
// When API requests SSF contracts, TWS sends both dividend protected and non-dividend protected contracts. They are indistinguishable except for having different conids.
// Fundamentals financial summary works from TWS but not from API 
// When requesting fundamental data for IBM, the data is returned but also an error
// The "Request option computation" method seems to have no effect; no data is ever returned
// When an order is submitted with the "End time" field, it seems to be ignored; it is not submitted but also no error is returned to API.
// Most error messages from TWS contain the class name where the error occurred which gets garbled to gibberish during obfuscation; the class names should be removed from the error message 
// If you exercise option from API after 4:30, TWS pops up a message; TWS should never popup a message due to an API request
// TWS did not desubscribe option vol computation after API disconnect
// Several error message are misleading or completely wrong, such as when upperRange field is < lowerRange
// Submit a child stop with no stop price; you get no error, no rejection
// When a child order is transmitted with a different contract than the parent but no hedge params it sort of works but not really, e.g. contract does not display at TWS, but order is working
// Switch between frozen and real-time quotes is broken; e.g.: request frozen quotes, then realtime, then request option market data; you don't get bid/ask; request frozen, then an option; you don't get anything
// TWS pops up mkt data warning message in response to api order

// API/TWS Changes
// we should add underConid for sec def request sent API to TWS so option chains can be requested properly
// reqContractDetails needs primary exchange, currently only takes currency which is wrong; all requests taking Contract should be updated
// reqMktDepth and reqContractDetails does not take primary exchange but it needs to; ideally we could also pass underConid in request
// scanner results should return primary exchange
// the check margin does not give nearly as much info as in TWS
// need clear way to distinguish between order reject and warning

// API Improvements
// add logging support
// we need to add dividendProt field to contract description
// smart live orders should be getting primary exchange sent down

// TWS changes
// TWS sends acct update time after every value; not necessary
// support stop price for trailing stop order (currently only for trailing stop-limit)
// let TWS come with 127.0.0.1 enabled, same is IBG
// we should default to auto-updating client zero with new trades and open orders

// NOTES TO USERS
// you can get all orders and trades by setting "master id" in the TWS API config
// reqManagedAccts() is not needed because managed accounts are sent down on login
// TickType.LAST_TIME comes for all top mkt data requests
// all option ticker requests trigger option model calc and response
// DEV: All Box layouts have max size same as pref size; but center border layout ignores this
// DEV: Box layout grows items proportionally to the difference between max and pref sizes, and never more than max size

//TWS sends error "Snapshot exceeds 100/sec" even when it doesn't; maybe try flush? or maybe send 100 then pause 1 second? this will take forever; i think the limit needs to be increased

//req open orders can only be done by client 0 it seems; give a message
//somehow group is defaulting to EqualQuantity if not set; seems wrong
//i frequently see order canceled - reason: with no text
//Missing or invalid NonGuaranteed value. error should be split into two messages
//Rejected API order is downloaded as Inactive open order; rejected orders should never be sen
//Submitting an initial stop price for trailing stop orders is supported only for trailing stop-limit orders; should be supported for plain trailing stop orders as well 
//EMsgReqOptCalcPrice probably doesn't work since mkt data code was re-enabled
//barrier price for trail stop lmt orders why not for trail stop too?
//All API orders default to "All" for F; that's not good
