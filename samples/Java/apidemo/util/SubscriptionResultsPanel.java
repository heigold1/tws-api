/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import apidemo.MarketDataPanel.ScannerResultsPanel;
import com.ib.client.ScannerSubscription;

/**
 *
 * @author heigold1
 */
public class SubscriptionResultsPanel {
    public ScannerSubscription m_sub;
    public ScannerResultsPanel m_resultsPanel;
    public int m_reqId; 
    
    public SubscriptionResultsPanel(int reqId, ScannerSubscription sub, ScannerResultsPanel resultsPanel)
    {
        m_sub = sub;
        m_resultsPanel = resultsPanel;
        m_reqId = reqId; 
    }
}
