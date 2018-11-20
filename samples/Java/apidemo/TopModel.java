/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;


import static com.ib.controller.Formats.fmt;
import static com.ib.controller.Formats.fmtPct;
import static com.ib.controller.Formats.*;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Formats;
import com.ib.controller.NewContract;
import com.ib.controller.NewTickType;
import com.ib.controller.Types.MktDataType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TopModel extends AbstractTableModel {
	public ArrayList<TopRow> m_rows = new ArrayList<TopRow>();
        Map<String, Integer> myMarketDataMap = new HashMap<String, Integer>();
        
	void addRow( NewContract contract) {
            String str_description = contract.description(); 
            String[] arr_orderParameters = str_description.split(" ");
            String str_symbol = arr_orderParameters[0];
            
		TopRow row = new TopRow( this, contract.description() );
		m_rows.add( row);
		int reqId = ApiDemo.INSTANCE.controller().reqTopMktData(contract, "", false, row);
                
                Integer requestId = reqId;
                
                myMarketDataMap.put(str_symbol, requestId);
                
		fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
	}

	void addRow( TopRow row) {
		m_rows.add( row);
		fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
	}

        public void removeAllRows()
        {
            this.desubscribe(); 
            m_rows.clear();
            fireTableRowsDeleted( 0, 0);
        }
        
        public void removeRow(int row) {
            m_rows.remove(row); 
            fireTableRowsDeleted(row, row);
        }

        public void checkResubscribe(){
            Iterator it = myMarketDataMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String symbolKey = (String) pair.getKey();

                System.out.println(pair.getKey() + " = " + pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
        
        public ArrayList<TopRow> getRows(){
            return m_rows; 
        }

	public void desubscribe() {
		for (TopRow row : m_rows) {
			ApiDemo.INSTANCE.controller().cancelTopMktData( row);
		}
	}		

	@Override public int getRowCount() {
		return m_rows.size();
	}
	
	@Override public int getColumnCount() {
		return 11;
	}
	
	@Override public String getColumnName(int col) {
		switch( col) {
			case 0: return "Description";
			case 1: return "Bid Size";
			case 2: return "Bid";
			case 3: return "Ask";
			case 4: return "Ask Size";
			case 5: return "Last";
			case 6: return "Time";
			case 7: return "Change";
			case 8: return "Volume";
                        case 9: return "Low"; 
                        case 10: return "Avg Volume"; 
			default: return null;
		}
	}

	@Override public Object getValueAt(int rowIn, int col) {
		TopRow row = m_rows.get( rowIn);
		switch( col) {
			case 0: return row.m_description;
			case 1: return row.m_bidSize;
			case 2: return fmt( row.m_bid);
			case 3: return fmt( row.m_ask);
			case 4: return row.m_askSize;
			case 5: return fmt( row.m_last);
			case 6: return fmtTime( row.m_lastTime);
			case 7: return row.change();
			case 8: return Formats.fmt0( row.m_volume);
                        case 9: return fmt( row.m_low);
                        case 10: return Formats.fmt0( row.m_avgVolume);
			default: return null;
		}
	}
	
	public void color(TableCellRenderer rend, int rowIn, Color def) {
		TopRow row = m_rows.get( rowIn);
		Color c = row.m_frozen ? Color.gray : def;
		((JLabel)rend).setForeground( c);
	}

	public void cancel(int i) {
		ApiDemo.INSTANCE.controller().cancelTopMktData( m_rows.get( i) );
	}
	
	static class TopRow extends TopMktDataAdapter {
		AbstractTableModel m_model;
		String m_description;
		double m_bid;
		double m_ask;
		double m_last;
		long m_lastTime;
		int m_bidSize;
		int m_askSize;
		double m_close;
		int m_volume;
                double m_low; 
                double m_avgVolume; 
		boolean m_frozen;
		
		TopRow( AbstractTableModel model, String description) {
			m_model = model;
			m_description = description;
		}

		public String change() {
			return m_close == 0	? null : fmtPct( (m_last - m_close) / m_close);
		}

		@Override public void tickPrice( NewTickType tickType, double price, int canAutoExecute) {
			switch( tickType) {
				case BID:
					m_bid = price;
					break;
				case ASK:
					m_ask = price;
					break;
				case LAST:
					m_last = price;
					break;
				case CLOSE:
					m_close = price;
					break;
				case LOW:
					m_low = price;
					break;
			}
			m_model.fireTableDataChanged(); // should use a timer to be more efficient
		}

		@Override public void tickSize( NewTickType tickType, int size) {
			switch( tickType) {
				case BID_SIZE:
					m_bidSize = size;
					break;
				case ASK_SIZE:
					m_askSize = size;
					break;
				case VOLUME:
					m_volume = size;
					break;
                                case AVG_VOLUME:         
                                        m_avgVolume = size; 
                                        break; 
			}
			m_model.fireTableDataChanged();
		}
		
		@Override public void tickString(NewTickType tickType, String value) {
			switch( tickType) {
				case LAST_TIMESTAMP:
					m_lastTime = Long.parseLong( value) * 1000;
					break;
			}
		}
		
		@Override public void marketDataType(MktDataType marketDataType) {
			m_frozen = marketDataType == MktDataType.Frozen;
			m_model.fireTableDataChanged();
		}
	}
        
        public static class MyCustomRow{
            public String m_symbol; 
            public double m_last; 
            public double m_change; 
            public int m_volume; 
            public double m_low; 
            public double m_avgVolume;
            
            public MyCustomRow(String symbol, double last, double change, int volume, double low, double avgVolume){
                m_symbol = symbol; 
                m_last = last;
                m_change = change;
                m_volume = volume; 
                m_low = low;
                m_avgVolume = avgVolume; 
            }
            
              private String name;
                // constructors and other methods omitted

              @Override
            public String toString() {
                return name;
            }
            
        }

}
