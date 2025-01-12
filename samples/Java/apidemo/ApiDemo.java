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
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

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
import javax.swing.ButtonGroup; 
import javax.swing.JRadioButton; 

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
import com.ib.controller.Types.OcaType; 
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApiDemo implements IConnectionHandler {
	static { NewLookAndFeel.register(); }
	static ApiDemo INSTANCE = new ApiDemo();

        public final double fl_secondOrderPercentageProfit = 0.05; 
        public final double fl_thirdOrderPercentageProfit = 3.5; 
        
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
                    
            if (str_status == "connected")
            {
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
                JButton m_noBracket = new JButton("No BR");
                JButton m_b29 = new JButton("2.6%");     
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
                JButton m_b20 = new JButton("20%"); 
                JButton m_b30 = new JButton("30%"); 
                JButton m_b40 = new JButton("40%"); 
                JButton m_b95 = new JButton("95%");
                JRadioButton m_separate12 = new JRadioButton("12%", true); 
                JRadioButton m_separate20 = new JRadioButton("20%"); 
                JCheckBox m_jaysAlgorithm = new JCheckBox("Jay's Alg"); 
                JCheckBox m_averageDown = new JCheckBox("3 Pt Avg"); 

                JTextField m_profitTakerHighRisk = new JTextField("3.5", 15);
                JTextField m_profitTakerNonHighRisk = new JTextField("4.2", 15);
                JButton m_processMultipleOrders = new JButton("Process Multiple Orders"); 
                
                public JTextArea m_multipleOrders = new JTextArea(20,40); 
                
                private final JLabel m_status = new JLabel("Disconnected");
		
                public void createOrders(double fl_percentProfit, double fl_secondaryPercentProfit, String str_multipleOrderString)
                {
                    JPanel p1 = new JPanel();
                                
                    int i_nextOrderId = 0; 

                    // grab the pasted order from the text box 
                    String str_orderText = ""; 
                    String str_tier = ""; 
                            
                    if (str_multipleOrderString.equals(""))
                    {
                    
                        try
                        {
                            str_orderText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor); 
                        }
                        catch(Exception e)
                        {
                            // if any I/O error occurs
                            System.out.println(e.getMessage()); 
                            e.printStackTrace();
                        }
                    }
                    else 
                    {
                        str_orderText = str_multipleOrderString; 
                        String[] parts = str_orderText.split(" "); 
                        str_tier = parts[7]; 
                        if (str_tier.equals("TWO_TIER"))
                        {
                            m_averageDown.setSelected(false); 
                            m_jaysAlgorithm.setSelected(true); 
                        }
                        else if (str_tier.equals("ONE_TIER"))
                        {
                            m_averageDown.setSelected(false); 
                            m_jaysAlgorithm.setSelected(false); 
                        }
                        
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
                    String str_price = arr_orderParameters[3].replaceAll("\\$", "");
                    double fl_price = Double.parseDouble(str_price); 

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

                    String str_previousClose = arr_orderParameters[6].replaceAll("\\$", "");
                    double fl_previousClose = Double.parseDouble(str_previousClose); 

                    
                    
                    
                    
                    // Three-tier orders 
                    if (m_averageDown.isSelected())
                    {
                        int i_averageDownSpread = 40;    
                        int i_averageDownPennySpread = 40; 

                        int i_averageDownStopOrderPercentage = 50;
                        System.out.println("Average down is selected.");
                        m_averageDown.setSelected(false);  
                        
                        // we are averaging down 

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

                        int i_parentFirstBuyOrderId = 0; 
                        int i_parentSecondBuyOrderId = 0; 
                        int i_parentThirdBuyOrderId = 0; 
                        int i_childFirstSellOrderId = 0;
                        int i_childSecondSellOrderId = 0;
                        int i_childThirdSellOrderId = 0;
                        int i_thirdOrderStopId = 0;

                        // grab the latest (max) order id
                        try
                        {
                            // new input stream created
                            FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                            BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                            String str_latestOrderId = fisReader.readLine();
                            i_parentFirstBuyOrderId = Integer.parseInt(str_latestOrderId);

                            System.out.println("The latest order id is " + i_parentFirstBuyOrderId);
                            fis.close();
                        }
                        catch(Exception e)
                        {
                            // if any I/O error occurs
                            System.out.println(e.getMessage()); 
                            e.printStackTrace();
                        }

                        o.orderId(i_parentFirstBuyOrderId); 
                        o.transmit(true);

                        System.out.println("The NEXT parent buy order id is " + i_parentFirstBuyOrderId);
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                        System.out.println("You have just sent off the parent order");

                        i_childFirstSellOrderId = i_parentFirstBuyOrderId + 1; 

                        NewOrder oSell = new NewOrder();
                        oSell.action(Action.SELL);
                        oSell.orderType(OrderType.LMT); 
                        double fl_childSellPrice = fl_price + fl_price*fl_percentProfit/100;

                        if (fl_childSellPrice >= 1.00)
                        {
                            fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                        }
                        else 
                        {
                            if (fl_previousClose >= 1.00)
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                            }
                            else
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                            }
                        }  

                        oSell.lmtPrice(fl_childSellPrice);
                        oSell.totalQuantity(i_numShares);
                        oSell.tif(TimeInForce.DAY);
                        oSell.outsideRth(true);
                        oSell.orderId(i_childFirstSellOrderId); 
                        oSell.parentId(i_parentFirstBuyOrderId);
                        oSell.ocaGroup(str_symbol + "_OCA_FIRST"); 
                        oSell.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                        System.out.println("You have just sent off the child sell order");

                        
                        i_nextOrderId =  i_childFirstSellOrderId + 1; // i_parentFirstBuyOrderId + 3;
                        i_parentSecondBuyOrderId = i_nextOrderId; 

                        
                        double fl_thirdBuyOrderEntryPrice = fl_price - fl_price*i_averageDownSpread/100; 
                        
                        if (fl_thirdBuyOrderEntryPrice < 1.00)
                        {
                            fl_thirdBuyOrderEntryPrice = fl_price - fl_price*i_averageDownPennySpread/100; 
                        }

                        double fl_secondBuyOrderEntryPrice = (fl_price + fl_thirdBuyOrderEntryPrice)/2; 

                        // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                        // so we have to adjust 
                        if (fl_price >= 1.00)
                        {
                            fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_secondBuyOrderEntryPrice )); 
                        }
                        else
                        {
                            if (fl_previousClose >= 1.00)
                            {
                                fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_secondBuyOrderEntryPrice )); 
                            }
                            else
                            {
                                fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.4f", fl_secondBuyOrderEntryPrice )); 
                            }
                        }

                        // 2nd parent buy order 

                        myContract = new NewContract();
                        myContract.symbol(str_symbol); 
                        myContract.secType(SecType.STK);
                        myContract.exchange("SMART"); 
                        myContract.primaryExch("ISLAND");
                        myContract.currency("USD"); 
                        NewOrder o2 = new NewOrder();

                        o2.account("U1203596"); 
                        o2.action(Action.BUY);
                        o2.orderType(OrderType.LMT);                                     

                        o2.lmtPrice(fl_secondBuyOrderEntryPrice);
                        o2.totalQuantity(i_numShares);
                        o2.tif(TimeInForce.DAY);
                        o2.outsideRth(true);
                        o2.orderId(i_parentSecondBuyOrderId); 
                        o2.ocaGroup(str_symbol + "_OCA_FIRST"); 
                        o2.ocaType(OcaType.CancelWithBlocking);  
                        o2.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o2); 

                        i_nextOrderId = i_parentSecondBuyOrderId + 1; 
                        i_childSecondSellOrderId = i_nextOrderId; 

                        // 2nd order's child sell order 

                        double fl_halfwayPrice = (fl_price + fl_secondBuyOrderEntryPrice)/2; 
                        
                        double fl_secondProfitTakerSellPrice = fl_halfwayPrice + fl_halfwayPrice*fl_secondOrderPercentageProfit/100; 

                        if (fl_secondProfitTakerSellPrice >  1.00)
                        {
                            fl_secondProfitTakerSellPrice = Double.parseDouble(String.format( "%.2f", fl_secondProfitTakerSellPrice )); 
                        }
                        else
                        {
                            if (fl_price >= 1.00)
                            {
                                fl_secondProfitTakerSellPrice = Double.parseDouble(String.format( "%.2f", fl_secondProfitTakerSellPrice )); 
                            }
                            else 
                            {
                                if (fl_previousClose >= 1.00)
                                {
                                    fl_secondProfitTakerSellPrice = Double.parseDouble(String.format( "%.2f", fl_secondProfitTakerSellPrice )); 
                                }
                                else
                                {
                                    fl_secondProfitTakerSellPrice = Double.parseDouble(String.format( "%.4f", fl_secondProfitTakerSellPrice )); 
                                }
                            }
                        }

                        NewOrder o2Sell = new NewOrder();
                        o2Sell.account("U1203596"); 
                        o2Sell.action(Action.SELL);
                        o2Sell.orderType(OrderType.LMT); 
//                        o2Sell.lmtPrice(fl_secondProfitTakerSellPrice);
                        o2Sell.lmtPrice(fl_childSellPrice);
                        o2Sell.totalQuantity(i_numShares);
                        o2Sell.tif(TimeInForce.DAY);
                        o2Sell.outsideRth(true);
                        o2Sell.orderId(i_childSecondSellOrderId); 
                        o2Sell.parentId(i_parentSecondBuyOrderId);
                        o2Sell.ocaGroup(str_symbol + "_OCA_SECOND"); 
                        o2Sell.transmit(true);

                        // Here we add the add the bracket orders twice because 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o2Sell); 
                        
/*
                        i_childSecondSellOrderId++; 
                        o2Sell.orderId(i_childSecondSellOrderId); 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o2Sell); 
*/

                        
                        i_nextOrderId = i_childSecondSellOrderId + 1; 
                        
                        // Now we go to the bottom (3rd) parent order
                        
                        i_parentThirdBuyOrderId = i_nextOrderId; 
                        
                        if (fl_thirdBuyOrderEntryPrice >= 1.00)
                        {
                            fl_thirdBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_thirdBuyOrderEntryPrice )); 
                        }
                        else 
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // so we have to adjust 
                            if (fl_price >= 1.00)
                            {
                                fl_thirdBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_thirdBuyOrderEntryPrice )); 
                            }
                            else 
                            {
                                if (fl_previousClose >= 1.00)
                                {
                                    fl_thirdBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_thirdBuyOrderEntryPrice )); 
                                }
                                else
                                {
                                    fl_thirdBuyOrderEntryPrice = Double.parseDouble(String.format( "%.4f", fl_thirdBuyOrderEntryPrice )); 
                                }
                            }
                        }  
                        
                        NewOrder o3 = new NewOrder();

                        o3.account("U1203596"); 
                        o3.action(Action.BUY);
                        o3.orderType(OrderType.LMT);                                     
                        o3.lmtPrice(fl_thirdBuyOrderEntryPrice);
                        o3.totalQuantity(i_numShares);
                        o3.tif(TimeInForce.DAY);
                        o3.outsideRth(true);
                        o3.orderId(i_parentThirdBuyOrderId); 
                        o3.ocaGroup(str_symbol + "_OCA_SECOND"); 
                        o3.ocaType(OcaType.CancelWithBlocking);  
                        o3.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3); 

                        i_nextOrderId = i_parentThirdBuyOrderId + 1; 

                        // 3rd order child profit taker 
                        
                        i_childThirdSellOrderId = i_nextOrderId; 

                        double fl_thirdProfitTakerSellPrice = fl_secondBuyOrderEntryPrice + fl_secondBuyOrderEntryPrice*fl_thirdOrderPercentageProfit/100; 

                        if (fl_thirdProfitTakerSellPrice >= 1.00)
                        {
                            fl_thirdProfitTakerSellPrice = Double.parseDouble(String.format( "%.2f", fl_thirdProfitTakerSellPrice )); 
                        }   
                        else
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // so we have to adjust 
                            if (fl_previousClose >= 1.00)
                            {
                                fl_thirdProfitTakerSellPrice = Double.parseDouble(String.format( "%.2f", fl_thirdProfitTakerSellPrice )); 
                            }
                            else 
                            {
                                fl_thirdProfitTakerSellPrice = Double.parseDouble(String.format( "%.4f", fl_thirdProfitTakerSellPrice )); 
                            }
                        }
                        
                        NewOrder o3Sell = new NewOrder();
                        o3Sell.account("U1203596"); 
                        o3Sell.action(Action.SELL);
                        o3Sell.orderType(OrderType.LMT); 
//                        o3Sell.lmtPrice(fl_thirdProfitTakerSellPrice);
                        o3Sell.lmtPrice(fl_childSellPrice);
                        o3Sell.totalQuantity(i_numShares);
                        o3Sell.tif(TimeInForce.DAY);
                        o3Sell.outsideRth(true);
                        o3Sell.orderId(i_childThirdSellOrderId); 
                        o3Sell.parentId(i_parentThirdBuyOrderId);
                        o3Sell.transmit(true);
                        
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3Sell); 

                        i_nextOrderId = i_childThirdSellOrderId + 1; 

                        /* I'm taking out the stop orders for now 
                        
                        i_thirdOrderStopId = i_nextOrderId; 

                        NewOrder o3Stop = new NewOrder();
                        o3Stop.account("U1203596"); 
                        o3Stop.action(Action.SELL);
                        o3Stop.orderType(OrderType.STP); 

                        double fl_childStopPrice;

                        fl_childStopPrice = fl_secondBuyOrderEntryPrice - fl_secondBuyOrderEntryPrice*i_averageDownStopOrderPercentage/100; 

                        if (fl_childStopPrice > fl_thirdBuyOrderEntryPrice)
                        {
                            double fl_thirdBuyStopOrderPercentage = fl_percentage +  12; 
                            fl_childStopPrice = fl_previousClose - (fl_thirdBuyStopOrderPercentage*fl_previousClose/100); 
                        }

                        if (fl_childStopPrice >= 1.00)
                        {
                            fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                        }
                        else 
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // so we have to adjust 
                            if (fl_previousClose >= 1.00)
                            {
                                fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                            }
                            else 
                            {
                                fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                            }
                        }  
                        
                        System.out.println("Child stop order price is " + fl_childStopPrice);
                        o3Stop.auxPrice(fl_childStopPrice);
                        o3Stop.totalQuantity(i_numShares);
                        o3Stop.tif(TimeInForce.DAY);
                        o3Stop.outsideRth(true);
                        o3Stop.orderId(i_thirdOrderStopId);
                        o3Stop.parentId(i_parentThirdBuyOrderId); 
                        o3Stop.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3Stop); 
                        i_thirdOrderStopId++; 
                        o3Stop.orderId(i_thirdOrderStopId); 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3Stop); 
                        i_thirdOrderStopId++; 
                        o3Stop.orderId(i_thirdOrderStopId); 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3Stop); 
                        System.out.println("You have just sent off the child stop order");                                    

                        i_nextOrderId = i_thirdOrderStopId + 1; 

                        */
                        
//                        m_averageDown.setSelected(false); 
//                         m_jaysAlgorithm.setSelected(true);
                    } // if we ARE averaging down 
                    else if (m_jaysAlgorithm.isSelected())
                    {
                        int i_stopOrderPercentage = 25;
                        
                        System.out.println("Jay's Algorithm Selected");                                    

                        m_jaysAlgorithm.setSelected(false);

                        int i_secondOrderPercentageDollar = 12; 
                        int i_secondOrderPercentagePenny = 15; 
                        
                        if (m_separate12.isSelected()){
                            i_secondOrderPercentageDollar = 12; 
                            i_secondOrderPercentagePenny = 15; 
                        }            
                        else if (m_separate20.isSelected()){
                            i_secondOrderPercentageDollar = 20; 
                            i_secondOrderPercentagePenny = 25; 
                        }                                

                        int i_firstOrderParent = 0; 
                        int i_firstOrderChildSell = 0; 
                        int i_secondOrderParent = 0; 
                        int i_breakEvenOrder = 0; 
                        int i_stopOrder = 0; 

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
                        o.lmtPrice(fl_price);
                        o.totalQuantity(i_numShares);
                        o.tif(TimeInForce.DAY);
                        o.outsideRth(true);

                        // grab the latest (max) order id
                        try
                        {
                            // new input stream created
                            FileInputStream fis = new FileInputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");
                            BufferedReader fisReader = new BufferedReader(new InputStreamReader(fis));

                            String str_latestOrderId = fisReader.readLine();
                            i_firstOrderParent = Integer.parseInt(str_latestOrderId);

                            System.out.println("The latest order id is " + i_firstOrderParent);
                            fis.close();
                        }
                        catch(Exception e)
                        {
                            // if any I/O error occurs
                            System.out.println(e.getMessage()); 
                            e.printStackTrace();
                        }

                        o.orderId(i_firstOrderParent); 
                        o.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o); 
                        System.out.println("You have just sent off the parent order");

                        i_firstOrderChildSell = i_firstOrderParent + 1; 

                        NewOrder oSell = new NewOrder();
                        oSell.action(Action.SELL);
                        oSell.orderType(OrderType.LMT); 
                        double fl_childSellPrice = fl_price + fl_price*fl_percentProfit/100;
                        
                        if (fl_childSellPrice >= 1.00)
                        {
                            fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                        }
                        else 
                        {

                            if (fl_previousClose >= 1.00)
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                            }
                            else
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                            }
                        }  

                        oSell.lmtPrice(fl_childSellPrice);
                        oSell.totalQuantity(i_numShares);
                        oSell.tif(TimeInForce.DAY);
                        oSell.outsideRth(true);
                        oSell.ocaGroup(str_symbol + "_OCA_FIRST"); 
                        oSell.orderId(i_firstOrderChildSell); 
                        oSell.parentId(i_firstOrderParent);
                        oSell.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                        System.out.println("You have just sent off the child sell order");

                        i_secondOrderParent = i_firstOrderChildSell + 1; 

                        // If it goes 12% past our original order, then we place our second order. 
                        double fl_secondBuyOrderEntryPrice = fl_price - fl_price*i_secondOrderPercentageDollar/100; 
                        
                        if (fl_secondBuyOrderEntryPrice < 1.00)
                        {
                            fl_secondBuyOrderEntryPrice = fl_price - fl_price*i_secondOrderPercentagePenny/100;
                        }

                        // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                        // so we have to adjust 
                        if (fl_secondBuyOrderEntryPrice >= 1.00)
                        {
                            fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_secondBuyOrderEntryPrice )); 
                        }
                        else
                        {
                            if (fl_previousClose >= 1.00)
                            {
                                fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.2f", fl_secondBuyOrderEntryPrice )); 
                            }
                            else
                            {
                                fl_secondBuyOrderEntryPrice = Double.parseDouble(String.format( "%.4f", fl_secondBuyOrderEntryPrice )); 
                            }
                        }
                        
                        // 2nd parent buy order 

                        myContract = new NewContract();
                        myContract.symbol(str_symbol); 
                        myContract.secType(SecType.STK);
                        myContract.exchange("SMART"); 
                        myContract.primaryExch("ISLAND");
                        myContract.currency("USD"); 
                        NewOrder o2 = new NewOrder();

                        o2.account("U1203596"); 
                        o2.action(Action.BUY);
                        o2.orderType(OrderType.LMT);                                     

                        o2.lmtPrice(fl_secondBuyOrderEntryPrice);
                        o2.totalQuantity(i_numShares);
                        o2.tif(TimeInForce.DAY);
                        o2.outsideRth(true);
                        o2.orderId(i_secondOrderParent); 
                        o2.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o2); 
                        System.out.println("You have just sent off the SECOND parent order");

                        i_breakEvenOrder = i_secondOrderParent + 1; 

                        double fl_breakEvenPrice = (fl_price + fl_secondBuyOrderEntryPrice)/2; 

                        double fl_breakEvenProfitPrice = fl_breakEvenPrice + fl_breakEvenPrice*fl_secondOrderPercentageProfit/100; 
                        
                        if (fl_breakEvenProfitPrice >= 1.00)
                        {
                            fl_breakEvenProfitPrice = Double.parseDouble(String.format( "%.2f", fl_breakEvenProfitPrice )); 
                        }
                        else 
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // so we have to adjust 
                            if (fl_previousClose >= 1.00)
                            {
                                fl_breakEvenProfitPrice = Double.parseDouble(String.format( "%.2f", fl_breakEvenProfitPrice )); 
                            }
                            else
                            {
                                fl_breakEvenProfitPrice = Double.parseDouble(String.format( "%.4f", fl_breakEvenProfitPrice )); 
                            }
                        }  
                        
                        NewOrder o3 = new NewOrder();

                        o3.account("U1203596"); 
                        o3.action(Action.SELL);
                        o3.orderType(OrderType.LMT);                                     
                        o3.lmtPrice(fl_breakEvenProfitPrice);
                        
                        // on the break-even order we are doubling the amount of shares sold, since we had to double the amount bought 
                        // in our second order 
                        o3.totalQuantity(i_numShares*2);
                        // we're putting it in the same OCA group as the 1st sell, in case it breaks even, then comes back up and hits this as a short
                        o3.ocaGroup(str_symbol + "_OCA_FIRST"); 
                        o3.tif(TimeInForce.DAY);
                        o3.outsideRth(true);
                        o3.orderId(i_breakEvenOrder); 
                        o3.parentId(i_secondOrderParent);
                        o3.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3); 
                        
                        // we're placing the break even order twice, because now we have double the shares from the first and second orders 
/*
                        i_breakEvenOrder++; 
                        o3.orderId(i_breakEvenOrder); 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, o3); 
*/

                        i_stopOrder = i_breakEvenOrder + 1;  
                        
                        double fl_stopPrice = fl_breakEvenPrice - fl_breakEvenPrice*i_stopOrderPercentage/100; 
                        
                        System.out.println("fl_stopPrice is " + fl_stopPrice);
                        
                        if (fl_stopPrice >= 1.00)
                        {
                            fl_stopPrice = Double.parseDouble(String.format( "%.2f", fl_stopPrice )); 
                        }
                        else 
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // so we have to adjust 
                            if (fl_price >= 1.00)
                            {
                                fl_stopPrice = Double.parseDouble(String.format( "%.2f", fl_stopPrice )); 
                            }
                            else 
                            {
                                if (fl_previousClose >= 1.00)
                                {
                                    fl_stopPrice = Double.parseDouble(String.format( "%.2f", fl_stopPrice )); 
                                }
                                else
                                {
                                    fl_stopPrice = Double.parseDouble(String.format( "%.4f", fl_stopPrice )); 
                                }
                            }
                        }  
                        
                        NewOrder oStop = new NewOrder();

                        oStop.account("U1203596"); 
                        oStop.action(Action.SELL);
                        oStop.orderType(OrderType.STP);                                     
                        oStop.auxPrice(fl_stopPrice);
                        oStop.totalQuantity(i_numShares);
                        oStop.tif(TimeInForce.DAY);
                        oStop.outsideRth(true);
                        oStop.orderId(i_stopOrder); 
                        oStop.parentId(i_secondOrderParent);
                        oStop.transmit(true);

                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                        System.out.println("You have just sent off the stop order");

                        i_stopOrder++;
                        oStop.orderId(i_stopOrder); 
                        ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oStop); 
                        
                        i_nextOrderId = i_stopOrder + 1; 

                        m_separate20.setSelected(false);
                        m_separate12.setSelected(true);
                        
                    }
                    else
                    {
                        int i_oneTierStopOrderPercentage = 20; 
                        
                        System.out.println("No averaging is selected");

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

                        
                        if (fl_price >= 1.00)
                        {
                            fl_price = Double.parseDouble(String.format( "%.2f", fl_price )); 
                        }
                        else 
                        {
                            if (fl_previousClose >= 1.00)
                            {
                                fl_price = Double.parseDouble(String.format( "%.2f", fl_price )); 
                            }
                            else
                            {
                                fl_price = Double.parseDouble(String.format( "%.4f", fl_price )); 
                            }
                        }

                        o.lmtPrice(fl_price);
                        o.totalQuantity(i_numShares);
                        o.tif(TimeInForce.DAY);
                        o.outsideRth(true);

                        // grab the latest (max) order id
                        int i_parentBuyOrderId = 0; 
                        int i_childSellOrderId = 0;
                        int i_childStopOrderId = 0;

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

                        double fl_childStopPrice;

                        fl_childStopPrice = fl_price - fl_price*i_oneTierStopOrderPercentage/100;

                        if (fl_childStopPrice >= 1.00)
                        {
                            fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                        }
                        else 
                        {
                            // if it's a dollar stock and we are then going to pennies, Interactive Brokers won't accept 4-digit penny prices
                            // if the previous day's closing price was over $1.00 
                            if (fl_previousClose >= 1.00)
                            {
                                fl_childStopPrice = Double.parseDouble(String.format( "%.2f", fl_childStopPrice )); 
                            }
                            else
                            {
                                fl_childStopPrice = Double.parseDouble(String.format( "%.4f", fl_childStopPrice )); 
                            }
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

                        // Limit sell bracket order off of the midway point

                        i_childSellOrderId = i_parentBuyOrderId + 2; 

                        NewOrder oSell = new NewOrder();
                        oSell.action(Action.SELL);
                        oSell.orderType(OrderType.LMT); 
                        double fl_childSellPrice = fl_price + fl_price*fl_percentProfit/100;
                        if (fl_childSellPrice >= 1.00)
                        {
                            fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                        }
                        else 
                        {
                            if (fl_previousClose >= 1.00)
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                            }
                            else
                            {
                                fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                            }
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

                    } // if (!m_averageDown.selected()) 
                    
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

                    if (str_multipleOrderString.equals(""))
                    {
                        javax.swing.JOptionPane.showMessageDialog(p1, str_orderText + "\n\nHas been placed successfully", "Order Placed", JOptionPane.NO_OPTION);
                    }
                }
                
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

                        // No bracket button 
                        
//                        m_jaysAlgorithm.setSelected(true); 
                        
                        m_averageDown.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                            // Handle the checkbox state change event
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    m_jaysAlgorithm.setSelected(false);
                                } else {

                                }
                            }
                        });
                        
                        m_jaysAlgorithm.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                            // Handle the checkbox state change event
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    m_averageDown.setSelected(false);
                                } else {

                                }
                            }
                        });
                        
                        m_separate12.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                            // Handle the checkbox state change event
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    m_separate20.setSelected(false);
                                } else {

                                }
                            }
                        });
                        
                        m_separate20.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                            // Handle the checkbox state change event
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    m_separate12.setSelected(false);
                                } else {

                                }
                            }
                        });

                        m_noBracket.addActionListener(new java.awt.event.ActionListener()
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

                                
                                
                                String str_previousClose;
                                double fl_previousClose = 0.00; 
                               
                                // If we have a previous day's closing price
                                if ((arr_orderParameters.length >= 7) && (arr_orderParameters[6] != null)) 
                                {    
                                    str_previousClose = arr_orderParameters[6].replaceAll("\\$", "");
                                    fl_previousClose = Double.parseDouble(str_previousClose); 
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

                                i_childSellOrderId = i_parentBuyOrderId + 1; 

                                NewOrder oSell = new NewOrder();
                                oSell.action(Action.SELL);
                                oSell.orderType(OrderType.LMT); 
                                // Since this is technically a "no bracket" order, we are just setting the profit to 40% 
                                double fl_childSellPrice = fl_price + fl_price*0.4;

                                // if we have a previous day's closing price
                                if ((arr_orderParameters.length >= 7) && (arr_orderParameters[6] != null)) 
                                {  
                                    if (fl_childSellPrice >= 1.00)
                                    {
                                        fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                    }
                                    else 
                                    {

                                        if (fl_previousClose >= 1.00)
                                        {
                                            fl_childSellPrice = Double.parseDouble(String.format( "%.2f", fl_childSellPrice )); 
                                        }
                                        else
                                        {
                                            fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                        }
                                    }  
                                }    
                                else 
                                {
                                    fl_childSellPrice = Double.parseDouble(String.format( "%.4f", fl_childSellPrice )); 
                                }

                                oSell.lmtPrice(fl_childSellPrice);
                                oSell.totalQuantity(i_numShares);
                                oSell.tif(TimeInForce.DAY);
                                oSell.outsideRth(true);
                                oSell.orderId(i_childSellOrderId); 
                                oSell.parentId(i_parentBuyOrderId);
                                oSell.transmit(true);

                                ApiDemo.INSTANCE.controller().m_client.placeOrder(myContract, oSell); 
                                System.out.println("You have just sent off the child sell order");

                                try
                                {
                                    // new input stream created
                                    FileOutputStream fos = new FileOutputStream("C:\\TWS API\\DayTradeApp\\latestOrder.txt");

                                    i_nextOrderId = i_childSellOrderId + 1; 
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

                                return;                                 

                	    } // end of the click "No BR" event handler
                        });      

                        // Take fast 2.6% 
                        m_b29.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(2.6, 1.5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 2.6%" 

                        // Take fast 3.5% 
                        m_b35.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(3.5, 2, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 3.5%" 

                        
                        // Take fast 4.2% 
                        m_b2.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(4.2, 3, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 4.2%" 

                        // Take fast 5% 
                        m_b3.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(5, 3.2, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 5%" 
                        
                        
                        // Take fast 5.15% 
                        m_b515.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(5.15, 3.2, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 5.15%" 
                        
                        

                        // Take fast 5.5% 
                        m_b55.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(5.5, 3.2, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 5.5%" 

                        // Take fast 6% 
                        m_b4.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(6, 4, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 6%" 
                        
                        // Take fast 7% 
                        m_b5.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(7, 4, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 7%"                    
                        
                        // Take fast 8% 
                        m_b6.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(8, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 8%"  
                        
                        // Take fast 9% 
                        m_b7.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(9, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 9%"  
                        
                        // Take fast 10% 
                        m_b8.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(10, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 10%"  
                        
                        // Take fast 11% 
                        m_b9.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(11, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 11%"  
                        
                        // Take fast 12% 
                        m_b10.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(12, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 12%"  

                        // Take fast 13% 
                        m_b11.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(13, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 13%"  

                        // Take 20% 
                        m_b20.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(20, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Take 20%"  

                        // Take 30%
                        m_b30.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(30, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Take  30%"
                        
                        // Take 40%
                        m_b40.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(40, 5, ""); 
                	    } 
                        });  // end of the click event handler for "Take 40%"
                        
                        // Take fast 95% 
                        m_b95.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_connectionPanel.createOrders(95, 50, ""); 
                	    } 
                        });  // end of the click event handler for "Fast 95%"  
                        
                        m_averageDown.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                m_jaysAlgorithm.setSelected(false); 
                	    } 
                        });  // end of the click event handler for "Fast 2.6%" 
                        

                        // grab the lines of orders from the text area and parse them out into orders.
                        m_processMultipleOrders.addActionListener(new java.awt.event.ActionListener()
                	{
                	   @Override public void actionPerformed(java.awt.event.ActionEvent evt)
                    	    {
                                // loop through the order strings and place the orders
                                for (String line : m_multipleOrders.getText().split("\\n")){

                                    line = line.trim(); 
                                    String[] parts = line.split(" "); 
                                    double fl_percentageProfit = Float.parseFloat(parts[8]); 
                                    fl_percentageProfit = Double.parseDouble(String.format( "%.2f", fl_percentageProfit ));
                                    
                                    m_connectionPanel.createOrders(fl_percentageProfit, 2.5, line); 
                                }
                                
                	    } // end of the @override function
                        });  // end of the click event handler for process multiple orders
                        
                        p1.add("Send:", m_noBracket); 
                        p1.add("Send:", m_b29); 
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
                        p1.add("Send:", m_b20); 
                        p1.add("Send:", m_b30);
                        p1.add("Send:", m_b40); 
                        p1.add("Send:", m_b95);
                        p1.add("", m_separate12);
                        p1.add("", m_separate20); 
                        p1.add("", m_jaysAlgorithm); 
                        p1.add("", m_averageDown); 
                       

			JPanel p2 = new VerticalPanel();
                        // p2.add(m_b2); 
			p2.add( connect);
			p2.add( disconnect);
			p2.add( Box.createVerticalStrut(20));

			JPanel p3 = new VerticalPanel();
			p3.setBorder( new EmptyBorder( 20, 0, 0, 0));
                        p3.add("High Risk:", m_profitTakerHighRisk);
                        p3.add("Non High Risk:", m_profitTakerNonHighRisk);
                        p3.add("", m_multipleOrders);
                        p3.add("", m_processMultipleOrders); 
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
