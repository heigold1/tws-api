/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ib.controller;

import com.ib.controller.NewContract;


/**
 *
 * @author heigold1
 */
public class ScannerRequestData {
    public NewContract m_contract; 
    public String m_genericTickList;
    public boolean m_snapshot; 
    
    public ScannerRequestData(NewContract contract, String genericTickList, boolean snapshot)
    {
        m_contract = contract; 
        m_genericTickList = genericTickList;
        m_snapshot = snapshot; 
    }
    
    public NewContract getNewContract(){return m_contract;} 
    public String getGenericTickList(){return m_genericTickList;} 
    public boolean getSnapshot(){return m_snapshot;} 
}