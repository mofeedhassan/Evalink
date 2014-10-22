package org.aksw;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.sql.*;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.SimpleJDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.FreeformQuery;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class MyVaadinUI extends UI
{
	
	// Data structures declarations
	//static Model model1 = ModelFactory.createDefaultModel(),model2=null,model4=null;
	//A data structure to contain the models holding the cahed data for each task
	static Map<Integer, Model> cachingModels = new HashMap<Integer, Model>();

	String userId="",userName="",task=""; //three variable to represent the user id, his name, and the task to work on
	//A data structure to cache endpoints
	HashMap<String, String> endpoints;
	// list holding file paths of cached data along with their task Ids
	Map<Integer, String> cahinfFiles;
	//Three panels to display for each tab 
	//{pnlLoginPanel: the login data, pnlEndpoints: show the tow endpoints ,pnlLinksDetails: show the details of each tak's links,pnlDisplayedPanel:main panel holding the others}
	Panel pnlDisplayedPanel,pnlLoginPanel,pnlLinksDetails,pnlEndpoints;
	//The main tab holding the other three subtabs to hold the three panels 
	TabSheet  tabMain=null;
	//tow comboboxes holding the endpoints lists
	ComboBox cmbSourceEndpoint, cmbDestinationEndpoint;
	ComboBox cmbUser, cmbTask;

	//list of suggested properties to check while investigating the links
	ListSelect lstSuggestedProperties ;
	

    @Override
    protected void init(VaadinRequest request) 
    {
    	//Create a panel for login data
    	pnlLoginPanel =new Panel("Login");
    	//Create a panel for endpoints
       	pnlEndpoints= new Panel();
    	pnlDisplayedPanel=designLoginPanel();
       	setContent(pnlDisplayedPanel); 
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    protected Panel designLoginPanel()
    {
    	//for login data and collecting users data
    	HashMap<String, String> users=null;
   
    	pnlLoginPanel.setWidth("100%");
    	pnlLoginPanel.setHeight("100%");
      	// Create absolute layout specifying its properties
    	final AbsoluteLayout loginLayout= new AbsoluteLayout();
    	loginLayout.setSizeFull();
    	// Create components Objects and specify their properties
    	try 
    	{
			users=getLoginInfo("jdbc:mysql://localhost:3306/","linkeval","root","mofo");
			endpoints=getEndpoints("jdbc:mysql://localhost:3306/", "linkeval","root","mofo");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//Design the login panel components
       	Button btnLogin = new Button("Login"); 
       	cmbUser = new ComboBox("Select your USER ID");
        cmbTask = new ComboBox("Select your TASK");
        
        cmbUser.setNullSelectionAllowed(false);
        cmbTask.setNullSelectionAllowed(false);
        //Add data
        for (Map.Entry<String, String> entry : users.entrySet())
        {
        	cmbUser.addItem(entry.getKey());
        	cmbUser.setItemCaption(entry.getKey(),entry.getValue());
        }
        cmbUser.setValue(cmbUser.getItemIds().iterator().next());
        
        //Listeners

        btnLogin.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
            	try
            	{
	            	userId = String.valueOf(cmbUser.getValue()); //which is his Id in tabel as this combo box shows names as captions and Ids as values
	            	userName=cmbUser.getItemCaption(userId);
	                task = String.valueOf(cmbTask.getValue());
	                if(task != "" && userId != "" )
	                {
	                	//Caching  the required data information 
	                	cachingForTriples();
	                	pnlEndpointsPanelDesign();
		            	pnlDisplayedPanel=pnlMainPanelDesign();
		            	pnlLinksDetails = allLinksDetails();
	
		            	tabMain = new TabSheet();
		            	tabMain.setSizeFull();
		            	VerticalLayout layout= new VerticalLayout();
		            	layout.addComponent(tabMain);
	
		            	tabMain.addTab(pnlEndpoints,"Task Endpoints");
		            	// Create the first tab
		            	tabMain.addTab(pnlLinksDetails, "Links Details"); 
		            	
		            	// Create the second tab
		            	// This tab gets its caption from the component caption
		            	tabMain.addTab(pnlDisplayedPanel,"Evaluating");
		            	
		            	setContent(tabMain);
	                }
            	}
            	catch(Exception e) {Notification.show(e.getMessage());}
            }
        });
        cmbUser.addFocusListener(new FocusListener() {
			
			@Override
			public void focus(FocusEvent event) {
				// TODO Auto-generated method stub
				cmbUser.setValue(cmbUser.getItemIds().iterator().next());
			}
		});
        cmbUser.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(final ValueChangeEvent event) 
            {
                final String user = String.valueOf(event.getProperty().getValue());
                
                try {
     				List<String>  tasks= getTasksInfo("jdbc:mysql://localhost:3306/","linkeval","root","mofo",user);
     				//clear the tasks combobox from any previous selections
     				cmbTask.removeAllItems();
     				if(tasks != null)
     				{
	     				for (String task : tasks) 
	     				{
	     					cmbTask.addItem(task);
	     				}
	     				cmbTask.setValue(cmbTask.getItemIds().iterator().next());
     				}	
     			} catch (SQLException e) {
     				// TODO Auto-generated catch block
     				Notification.show("Error in accessing User's tasks\n"+e.getMessage()) ;
     			}
            }
        });

        loginLayout.addComponent(cmbUser, "left: 800px; top: 200px;");
        loginLayout.addComponent(cmbTask, "left: 800px; top: 300px;");
        loginLayout.addComponent(btnLogin, "left: 900px; top: 400px;");

        pnlLoginPanel.setContent(loginLayout);
    	
       	return pnlLoginPanel;
    }
 ///////////////////////////////////////////////////////////////////////////////////////////   
   
    Label lblSource,lblDestination;
    Link lnkSource=new Link();
    Link lnkDestination = new Link();
    
    Table tblSourceDestinationparam,tblSourcePropertiesParam,tblDestinationPropertiesParam; //pass tables between different panels
    long lStartTime =0;
	long lEndTime=0; 
	boolean newLink=true;
	Table tblSourceDestination = new Table("Source and Destination URIs");
    protected Panel allLinksDetails()
    {

    	//create the panel that will hold all components
    	Panel linksDetails = new Panel("Links Details");
    	//set panel's properties   	
    	linksDetails.setSizeFull();
    	
      	// Create absolute layout specifying its properties
    	final GridLayout layout = new GridLayout(1,2);
    	//set layout's properties
    	layout.setSizeFull();
     	
    	// Create components Objects and specify their properties
       	Button btnLoad = new Button("Load task");
    	//final Table tblSourceDestination = new Table("Source and Destination URIs");
     	tblSourceDestination.setSelectable(true);     	
     	tblSourceDestination.setSizeFull();    	
    	//fill the Source and Destination URIs table
    	SQLContainer container=connectToDB("root", "mofo",userName);
    	Notification.show("Welcome "+userName+" you loaded task Nr.: "+task);
    	Compare.Equal suburbFilter = new Compare.Equal("taskId",Integer.valueOf(task));
    	container.addContainerFilter(suburbFilter);
    	//Fill the main (tblSourceDestination) table with resources
    	tblSourceDestination.setContainerDataSource(container);
    	
    	tblSourceDestinationparam=tblSourceDestination;
    	
    	// add component to the layout specifying its position on the layout
    	int tableWidth = (int) tblSourceDestination.getWidth();
    	layout.addComponent(btnLoad,0,1);
    	layout.addComponent(tblSourceDestination,0,0);
    	layout.setComponentAlignment(btnLoad, Alignment.TOP_LEFT);
    	layout.setSpacing(false);
    	linksDetails.setContent(layout);
    	// Add EventHandlers for some of the components
    	
    	
    	btnLoad.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
     
            	try
            	{
	            	Object rowId =tblSourceDestination.getValue();
	                Property sourceProperty=tblSourceDestination.getContainerProperty(rowId,"sourceURI");
	                Property destinationProperty=tblSourceDestination.getContainerProperty(rowId,"destinationURI");
	                
	              /*  lblSource.setValue(sourceProperty.toString());
	                lblDestination.setValue(destinationProperty.toString());*/
	                
	                lnkSource.setResource(new ExternalResource(sourceProperty.toString()));
	                lnkDestination.setResource(new ExternalResource(destinationProperty.toString()));
	                
	                lnkSource.setCaption(sourceProperty.toString());
	            	lnkDestination.setCaption(destinationProperty.toString());

	                tblSourcePropertiesParam.removeAllItems();
	                tblDestinationPropertiesParam.removeAllItems();
	                
	                tabMain.setSelectedTab(2);
	                
	                Notification.show("Links' URIs are successfully loaded ");
            	}
            	catch(Exception e)
            	{
            		Notification.show("You did not select an item in the links table");
            	}

            }
        });
    	
    	tblSourceDestination.addItemClickListener(new ItemClickEvent.ItemClickListener() {

            public void itemClick(ItemClickEvent event) 
            {

            }
        });
    	
    	
     	return linksDetails;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////
    protected void cachingForTriples()
    {
    	//get caching files location from DB
    	String queryCach="SELECT taskId,caching FROM Tasks";
    	SQLContainer cachedFile =connectToDB("root", "mofo","linkeval",queryCach);
    	hashingCaching(cachedFile);
    	Model cachedModel=null;
    	int t = Integer.parseInt(task);
    	if((cahinfFiles.keySet().contains(t)))// the file exists
    	{		

    		try
    		{
	    		cachedModel=org.aksw.Reader.readModel( cahinfFiles.get(t) );
	    		cachingModels.put(t, cachedModel);
    		}
    		catch(Exception e)
    		{
    			Notification.show("Caching File not exist");//not reached at all casuse of if
    		}
    	}


    	/*//get cahing files location from DB
    	String queryCach="SELECT taskId,caching FROM Tasks";
    	SQLContainer cachedFile =connectToDB("root", "mofo","TL2",queryCach);
    	hashingCaching(cachedFile);
    	if(task.equals("1") && model1 == null)
    		model1= org.aksw.Reader.readModel( cahinfFiles.get(Integer.parseInt(task)) );
    	else if(task.equals("2") && model2 == null)
    		model2= org.aksw.Reader.readModel( cahinfFiles.get(Integer.parseInt(task)) );
    	else if(task.equals("4") && model4 == null)
    		model4= org.aksw.Reader.readModel( cahinfFiles.get(Integer.parseInt(task)) );*/
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////
    protected SQLContainer connectToDB (String userName, String passWord, String DB, String query)
    {
    	SimpleJDBCConnectionPool connectionPool;  
    	SQLContainer container=null;
        try
        {
        	connectionPool = new SimpleJDBCConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/"+DB,userName, passWord, 1,7);
        	FreeformQuery ask = new FreeformQuery(query, connectionPool);
        	container = new SQLContainer(ask);
        }
        catch(Exception e)
        {
        	Notification.show(e.getMessage());
        }
        return container;
    }
    
    protected void hashingCaching(SQLContainer cahingContainer)
    {
    	cahinfFiles= new HashMap<Integer, String>();
    	for (int i = 0; i < cahingContainer.size(); i++) 
    	{
    	    Object id = cahingContainer.getIdByIndex(i);
    	    Item item = cahingContainer.getItem(id);
    	    // do stuff with item
    	    Integer taskId=  (Integer)  item.getItemProperty("taskId").getValue();
    	    String caching =item.getItemProperty("caching").getValue().toString();
    	    File f = new File(caching);
    	    if(f.exists()) {  cahinfFiles.put(taskId, caching);}//add only the files you are sure they are exist in the specified path
    	    
    	}
    	
    }
    protected SQLContainer connectToDB(String userName, String passWord, String table)
    {
    	SimpleJDBCConnectionPool connectionPool;  
    	SQLContainer container=null;
        try
        {
        	connectionPool = new SimpleJDBCConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/linkeval",userName, passWord, 1,7);
        	TableQuery tq = new TableQuery(table, connectionPool);
        	container = new SQLContainer(tq);
        }
        catch(Exception e)
        {
        	Notification.show(e.getMessage());
        }
        return container;
    }
    
       
    
    protected SQLContainer connectToDBFreeQuery(String userName, String passWord)
    {//has problems
    	SimpleJDBCConnectionPool connectionPool;  
    	SQLContainer container=null;
        try
        {
        	connectionPool = new SimpleJDBCConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/linkeval",userName, passWord, 1,7);
        	FreeformQuery query = new FreeformQuery("SELECT property " +
        											"FROM Properties AS P,PropertiesSuggestions AS S" +
        											" WHERE P.Id = S.propertyId AND S.taskId = "+task, connectionPool);
        	container = new SQLContainer(query);
        }
        catch(Exception e)
        {
        	Notification.show("Database problem in accessing important properties of current task"+e.getMessage());
        }
        return container;
    }
 
    //////////////////////////////////////////////////////////////////////////////////////////

    protected Panel pnlMainPanelDesign()
    {
  
    	//create the panel that will hold all components
    	
    	Panel pnlURIsProperties = new Panel("URI Display");
    	   	
    	pnlURIsProperties.setWidth("100%");
    	pnlURIsProperties.setHeight("100%");
    	
      	// Create absolute layout specifying its properties
    	final GridLayout layout = new GridLayout(1,3);//main layout in the page 3 rows, 1 col
    	GridLayout lytProperties= new GridLayout(2, 1);//split the its docked row into 2 columns (each for a properties table) , 1 row 
    	GridLayout lytDecision_Others= new GridLayout(3,1);//split other details to be 1 row, 3 col left for source label, middle for properties and buttons,right for destination label
    	GridLayout lytButtons_List= new GridLayout(1,2);
    	
    	
    	HorizontalLayout lytButtonsSub= new HorizontalLayout();
    	lytProperties.setSizeFull();
     	layout.setSizeFull();
     	
    	// Create components Objects and specify their properties
       	
    	Button btnCorrect = new Button("Correct");
    	Button btnIncorrect = new Button("Incorrect");
    	Button btnUnsure = new Button("Unsure");
    	Button btnGetProperties= new Button("Get properties");
    	
    	  
    	// to load properties of loaded resources automatically
    	final CheckBox chkAutomaticPropertiesLoad = new CheckBox("Automatic Properties loading (next time)");
    	chkAutomaticPropertiesLoad.setValue(false);// Unchecked by default
    	
    	

    	/*lblSource= new Label("Source URI");
    	lblDestination= new Label("Destination URI");*/
    	
    	  	
    	final Table tblSourcePropertiesMapping = new Table("Source Properties");
    	final Table tblDestinationPropertiesMapping = new Table("Destination Properties");
    	tblSourcePropertiesParam=tblSourcePropertiesMapping;
    	tblDestinationPropertiesParam=tblDestinationPropertiesMapping;
    	
    	tblSourcePropertiesMapping.setSizeFull();    	
    	tblDestinationPropertiesMapping.setSizeFull();  
    	tblSourcePropertiesMapping.setSelectable(true);
    	tblDestinationPropertiesMapping.setSelectable(true);

    	tblSourcePropertiesMapping.addContainerProperty("Property", String.class,  null);
    	tblSourcePropertiesMapping.addContainerProperty("Value",  String.class,  null);

    	tblDestinationPropertiesMapping.addContainerProperty("Property",  String.class,  null);
    	tblDestinationPropertiesMapping.addContainerProperty("Value",  String.class,  null);
    	tblDestinationPropertiesMapping.setMultiSelect(true);
    	
       	lstSuggestedProperties = new ListSelect("Lookup Properties");
    	lstSuggestedProperties.setRows(4);
    	lstSuggestedProperties.setNullSelectionAllowed(false);
    	lstSuggestedProperties.setSizeFull();

    	//set the tasks' endpoints to be selected
    	List<String> endpointsIDs=getTaskEndpointsIDs("jdbc:mysql://localhost:3306/", "linkeval","root","mofo");
    	HashMap<String, Object> endpointsComponents = collectComponents("SourceEP",cmbSourceEndpoint,"DestinationEP",cmbDestinationEndpoint);
        setTaskEndpoints(endpointsIDs, endpointsComponents);
		

    	SQLContainer lstContainer =connectToDBFreeQuery("root", "mofo");
    	int lstSize=lstContainer.size();
    	int i=0;
        for (Object cityItemId : lstContainer.getItemIds()) 
        {
        	lstSuggestedProperties.addItem(i);
        	String g=lstContainer.getItem(cityItemId).getItemProperty("property").getValue().toString();
        	lstSuggestedProperties.setItemCaption(i,g);
        	i++;
        }
        lstSuggestedProperties.setValue(lstSuggestedProperties.getItemIds().iterator().next());
    	
    	btnCorrect.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
            	//list of Components will be affected
            	HashMap<String, Object> components =null;
            	components=collectComponents("chkProperties",chkAutomaticPropertiesLoad,"cmbSourceEP",cmbSourceEndpoint,"cmbDestinationEP",cmbDestinationEndpoint,
            			"tblSourceProp",tblSourcePropertiesMapping,"tblDestinationProp",tblDestinationPropertiesMapping);
            	if(components != null)
            		decisionButtonAction("Correct",components);
            	else
            		Notification.show("Button Correct is Out of Action");
             }
        });
    	btnIncorrect.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
            	//list of Components will be affected
            	HashMap<String, Object> components =null;
            	components=collectComponents("chkProperties",chkAutomaticPropertiesLoad,"cmbSourceEP",cmbSourceEndpoint,"cmbDestinationEP",cmbDestinationEndpoint,
            			"tblSourceProp",tblSourcePropertiesMapping,"tblDestinationProp",tblDestinationPropertiesMapping);
            	//do action
            	if(components != null)
            		decisionButtonAction("Incorrect",components);
            	else
            		Notification.show("Button Incorrect is Out of Action"); 
            }
        });
    	btnUnsure.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
            	//list of Components will be affected
            	HashMap<String, Object> components =null;
            	components=collectComponents("chkProperties",chkAutomaticPropertiesLoad,"cmbSourceEP",cmbSourceEndpoint,"cmbDestinationEP",cmbDestinationEndpoint,
            			"tblSourceProp",tblSourcePropertiesMapping,"tblDestinationProp",tblDestinationPropertiesMapping);
            	//do action
            	if(components != null)
            		decisionButtonAction("Unsure",components);
            	else
            		Notification.show("Button Unsure is Out of Action");
            }
        });
    	/////////////////////////////////////////////////Get Properties of the loaded URI////////////////////////////////////
    	btnGetProperties.addClickListener(new Button.ClickListener() 
        {
            public void buttonClick(ClickEvent event) 
            {
            	try
            	{
            	HashMap<String, Object> components =null;
            	components=collectComponents("chkProperties",chkAutomaticPropertiesLoad,"cmbSourceEP",cmbSourceEndpoint,"cmbDestinationEP",cmbDestinationEndpoint,
            			"tblSourceProp",tblSourcePropertiesMapping,"tblDestinationProp",tblDestinationPropertiesMapping);
            	//do action
            	if(components != null)
            		loadURIsProperties(components);
            	else
            		Notification.show("Problem in getting Properties");
            	}
            	catch(Exception e) {Notification.show("No selected URIs");}
          }
        });
    	
    	lstSuggestedProperties.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(final ValueChangeEvent event) 
            {
                final String valueString = String.valueOf(event.getProperty().getValue());
                List<Object> Ids= new ArrayList<Object>();
                Object first=null;
                for (Iterator i = tblSourcePropertiesMapping.getItemIds().iterator(); i.hasNext();) 
			    {
			        // Get the current item identifier, which is an integer.
                	first=i.next();
			        int iid = (Integer) first;
			        String other=tblSourcePropertiesMapping.getItem(iid).getItemProperty("Property").toString();
			        if(other.equals(valueString))   //if(other.equals(property))
			        {
			        	Ids.add(iid);
			        	break;
			        }
			    }
                tblSourcePropertiesMapping.setImmediate(true);
                tblSourcePropertiesMapping.setValue(Ids);
				tblDestinationPropertiesMapping.setCurrentPageFirstItemId(first);
            }
        });

    	tblSourcePropertiesMapping.addItemClickListener(new ItemClickEvent.ItemClickListener() {

            public void itemClick(ItemClickEvent event) 
            {
            	String property=tblSourcePropertiesMapping.getContainerProperty(event.getItemId(), event.getPropertyId()).toString();
                List<String> res= getRelatedProperties(property);
                
                if(res==null)
                {
                	Notification.show("No related Properties");
                	return;
                }
                boolean Found=false;
                List<Object> Ids= new ArrayList<Object>();
                Object first=null;
                int x=0;
				for (String relatedProperty : res) 
				{
				    for (Iterator i = tblDestinationPropertiesMapping.getItemIds().iterator(); i.hasNext();) 
				    {
				        // Get the current item identifier, which is an integer.
				    	Object theId=i.next();
				        int iid = (Integer) theId;
				        String other=tblDestinationPropertiesMapping.getItem(iid).getItemProperty(event.getPropertyId()).toString();
				    	
				        if(other.equals(relatedProperty))   //if(other.equals(property))
				        {
				        	Ids.add(iid);
				        	if(x==0)
				        	{
				        		first=theId;
				        		x=1;
				        	}
				        	Found=true;				        	
				        }
				    }
				}
				if(!Found)
					Notification.show("Related property is not Found in destination table try manual search");
				else
				{
					Notification.show("Found in destination table");
					tblDestinationPropertiesMapping.setValue(Ids);
					tblDestinationPropertiesMapping.setCurrentPageFirstItemId(first);
				}
            }
        });
    	
    
    	
    	tblDestinationPropertiesMapping.addItemClickListener(new ItemClickEvent.ItemClickListener() {

            public void itemClick(ItemClickEvent event) 
            {
            	String property=tblDestinationPropertiesMapping.getContainerProperty(event.getItemId(), event.getPropertyId()).toString();
                List<String> res= getRelatedProperties(property);
                
                if(res==null)
                {
                	Notification.show("No related Properties");
                	return;
                }
                boolean Found=false;
                List<Object> Ids= new ArrayList<Object>();
                Object first=null;
                int x=0;
				for (String relatedProperty : res) 
				{
				    for (Iterator i = tblSourcePropertiesMapping.getItemIds().iterator(); i.hasNext();) 
				    {
				        // Get the current item identifier, which is an integer.
				    	Object theId=i.next();
				        int iid = (Integer) theId;
				        String other=tblSourcePropertiesMapping.getItem(iid).getItemProperty(event.getPropertyId()).toString(); 
				    	
				        if(other.equals(relatedProperty))   //if(other.equals(property))
				        {
				        	Ids.add(iid);
				        	if(x==0)
				        	{
				        		first=theId;
				        		x=1;
				        	}
				        	Found=true;				        	
				        }
				    }
				}
				if(!Found)
					Notification.show("Related property is not Found in destination table try manual search");
				else
				{
					Notification.show("Found in source table");
					tblSourcePropertiesMapping.setValue(Ids);  
					tblSourcePropertiesMapping.setCurrentPageFirstItemId(first);   
				}
            }
        });
    	// add component to the layout specifying its position on the layout
    	
    	
    	lytProperties.addComponent(tblSourcePropertiesMapping,0,0);
    	lytProperties.addComponent(tblDestinationPropertiesMapping,1,0);
    	lytProperties.setSizeFull(); 
    	
    	lytButtonsSub.addComponent(btnCorrect);
    	lytButtonsSub.addComponent(btnIncorrect);
    	lytButtonsSub.addComponent(btnUnsure);
    	lytButtonsSub.addComponent(btnGetProperties);
    	
    	lytButtons_List.addComponent(lstSuggestedProperties, 0, 0); 
    	lytButtons_List.addComponent(lytButtonsSub, 0, 1);
    	
    	lytButtons_List.setSizeFull();
    	lytButtons_List.setComponentAlignment(lstSuggestedProperties, Alignment.TOP_CENTER);
    	lytButtons_List.setComponentAlignment(lytButtonsSub, Alignment.MIDDLE_CENTER);

    	
    	
    	lytDecision_Others.addComponent(lytButtons_List,1,0);
    	/*lytDecision_Others.addComponent(lblSource,0,0);
    	lytDecision_Others.addComponent(lblDestination,2,0);
    	lytDecision_Others.setComponentAlignment(lblSource, Alignment.TOP_LEFT);
    	lytDecision_Others.setComponentAlignment(lblDestination, Alignment.TOP_RIGHT);*/
    	lnkSource.setTargetName("_blank");
    	lnkDestination.setTargetName("_blank");

    	lytDecision_Others.addComponent(lnkSource,0,0);
    	lytDecision_Others.addComponent(lnkDestination,2,0);
    	lytDecision_Others.setComponentAlignment(lnkSource, Alignment.TOP_LEFT);
    	lytDecision_Others.setComponentAlignment(lnkDestination, Alignment.TOP_RIGHT);
    	
    	
    	lytDecision_Others.setSizeFull();
    	
    	layout.addComponent(lytProperties,0,0,0,1);
    	layout.addComponent(lytDecision_Others, 0, 2);


    	
    	pnlURIsProperties.setContent(layout);
    	
    	return pnlURIsProperties;
    }
	
    protected void pnlEndpointsPanelDesign()
    {
    	VerticalLayout lytEndpoints = new VerticalLayout();
    	cmbSourceEndpoint= new ComboBox("Source Endpoint");
    	cmbDestinationEndpoint= new ComboBox("Destination Endpoint");
    	cmbSourceEndpoint.setNullSelectionAllowed(false);
    	cmbSourceEndpoint.setTextInputAllowed(false);
    	cmbDestinationEndpoint.setNullSelectionAllowed(false);
    	cmbDestinationEndpoint.setTextInputAllowed(false);
    	cmbSourceEndpoint.setNullSelectionAllowed(false);
    	cmbDestinationEndpoint.setNullSelectionAllowed(false);
 
    	///get data for comboboxes
    	SQLContainer cmbContainer= connectToDB("root", "mofo","Endpoints");
    	//fill endpoints
		cmbSourceEndpoint.setContainerDataSource(cmbContainer);
       	cmbDestinationEndpoint.setContainerDataSource(cmbContainer);  
    	//select tasks endpoint to be default
       	//getTaskEndpoint
    	cmbSourceEndpoint.setValue(cmbSourceEndpoint.getItemIds().iterator().next());
    	cmbDestinationEndpoint.setValue(cmbDestinationEndpoint.getItemIds().iterator().next());
    	lytEndpoints.addComponent(cmbSourceEndpoint);
    	lytEndpoints.addComponent(cmbDestinationEndpoint);
    	lytEndpoints.setComponentAlignment(cmbSourceEndpoint, Alignment.MIDDLE_CENTER);
    	lytEndpoints.setComponentAlignment(cmbDestinationEndpoint, Alignment.MIDDLE_CENTER);
    	lytEndpoints.setSizeFull();


    	
    	pnlEndpoints.setContent(lytEndpoints);
    	pnlEndpoints.setSizeFull(); 
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected HashMap<String, Object> collectComponents(Object...objects)
    {
    	HashMap<String, Object> components=null;
    	if(objects.length > 0 && (objects.length % 2 == 0))// not odd value
    	{
    		components= new HashMap<String, Object>();
    		for(int i=0;i<objects.length;i++)
    		{
    			String key=(String) objects[i++];
    			Object value=objects[i];
    			components.put(key, value);
    		}
    	}
    	return components;
    }
    
  
    
    protected void setDecisionAndTime(String decision)
    {
    	//get the selected item's Id from the big table (SourceDEstination)
		Object rowId =tblSourceDestinationparam.getValue();
		//calculate end of time for the ended interval of taking decision in milliseconds
		lEndTime = System.currentTimeMillis();
		long lEllapsedTime=lEndTime-lStartTime;
		//enable the big table to be editable so to set values in its table components
		tblSourceDestination.setEditable(true);
		tblSourceDestination.getContainerProperty(rowId, "decision").setValue(decision); 
		tblSourceDestination.getContainerProperty(rowId, "time").setValue(String.valueOf(lEllapsedTime)); 
		tblSourceDestination.setEditable(false);
		//retrieving a reference to the data container to save the value directly in its table in the database
		SQLContainer c =(SQLContainer) tblSourceDestination.getContainerDataSource();
		try {
			c.commit();
		} catch (/*UnsupportedOperationException |*/ SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    protected void loadURIsProperties(HashMap<String, Object> components)
    {
    	//loading the properties of the URI's
    	
        if(true)// Is the automatic button checked?
        {
        	//load the properties automatically
        	String sourceEndpoint="",destinationEndpoint="";
        	sourceEndpoint=((ComboBox)(components.get("cmbSourceEP"))).getItemCaption(((ComboBox)(components.get("cmbSourceEP"))).getValue());
        	destinationEndpoint=((ComboBox)(components.get("cmbDestinationEP"))).getItemCaption(((ComboBox)(components.get("cmbDestinationEP"))).getValue());
        	String subject="";
        	try
    	  	{
        		//subject=lblSource.getValue();
        		subject=lnkSource.getCaption();
	            getURIProperties(subject,sourceEndpoint,((Table)(components.get("tblSourceProp"))));
	            
	            //subject=lblDestination.getValue();	
	            subject=lnkDestination.getCaption();
	            getURIProperties(subject,destinationEndpoint,((Table)(components.get("tblDestinationProp"))));
    	  	}
        	catch(Exception e){Notification.show("ERROR in loading "+subject+" URIs properties");}
        	lStartTime= System.currentTimeMillis();
        	//start time for next one
        }
    }
    protected void loadURIs()
    {
    	try
    	{
			// get the row item's Id from the table
        	Object rowId =tblSourceDestination.getValue();
            Property sourceProperty=tblSourceDestination.getContainerProperty(rowId,"sourceURI");//get the source's URI
            Property destinationProperty=tblSourceDestination.getContainerProperty(rowId,"destinationURI");//get the target's URI
            //set the source and destination labels to selected URI's 
            /*lblSource.setValue(sourceProperty.toString());
            lblDestination.setValue(destinationProperty.toString());*/
            lnkSource.setResource(new ExternalResource(sourceProperty.toString()));
            lnkDestination.setResource(new ExternalResource(destinationProperty.toString()));
            
            lnkSource.setCaption(sourceProperty.toString());
        	lnkDestination.setCaption(destinationProperty.toString());
        	
            //clear all URI's properties table
            tblSourcePropertiesParam.removeAllItems();
            tblDestinationPropertiesParam.removeAllItems();
    	}
    	catch(Exception e)
    	{
    		Notification.show("You did not select an item in the links table");
    	}
    }
    protected void setTaskEndpoints(List<String> endpointsIDs , HashMap<String, Object> endpointsComponents)
    {
    	//get values
    	String sourceEP=endpointsIDs.get(0);
    	String desstinationEP=endpointsIDs.get(1);
    	//get components
    	ComboBox source = (ComboBox)endpointsComponents.get("SourceEP");
    	ComboBox destination = (ComboBox)endpointsComponents.get("DestinationEP");
    	//set their values
    	Collection IDs=  source.getItemIds();
		for (Object id : IDs) 
		{
			if(source.getItemCaption(id).trim().equals(endpoints.get(sourceEP).trim()))
			{
				source.setValue(id);
				break;
			}
		}
		IDs=  destination.getItemIds();
		for (Object id : IDs) 
		{
			if(destination.getItemCaption(id).trim().equals(endpoints.get(desstinationEP).trim()))
			{
				destination.setValue(id);
				break;
			}
		}
    }
    protected void decisionButtonAction(String decision, HashMap<String, Object> components)
    {
    	try
    	{
    	//1- Assign decision and time
    		setDecisionAndTime(decision);
    		//Notification.show("Decision "+ decision+ " is made");
    	//2- Advance and load new URIs
    		//get reference to the big table's container
    		SQLContainer s=(SQLContainer) tblSourceDestination.getContainerDataSource();
    		//check if it is not the last item in the table so you can advance
    		if(!(tblSourceDestination.getValue().equals(tblSourceDestination.lastItemId())))
    		{
    			//get the index of the selected item in the big table
    			int index=s.indexOfId(tblSourceDestination.getValue());
    			//advance to the next item's index and make it selected
    			index++;
    			tblSourceDestination.setValue(s.getIdByIndex(index));
    			// Load the new selected URI's into their labels(=load btn)
    			loadURIs();
    	//3- Load the URI's Properties
    			loadURIsProperties(components);
    		}
    	}
    	catch(Exception e){Notification.show("Unable to load the URI properties after the decision"+e.getMessage());}
    }
    protected List<String> getRelatedProperties(String property)
    {
    	Connection con = null;
		String selectQuery= "select property from Properties where " +
				"(Id in (select firstProperty from propertyMappings " +
						"where secondProperty = (select Id from Properties Where property =\""+property+"\")))" +
				" OR " +
				"(Id in (select secondProperty from propertyMappings " +
						"where firstProperty = (select Id from Properties Where property =\""+property+"\")))";
		String driver = "com.mysql.jdbc.Driver";
		ResultSet linksRecords=null;
		List<String> relatedProperties= new ArrayList<String>();
		  try
		  {
			  try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  con = DriverManager.getConnection("jdbc:mysql://localhost:3306/linkeval","root","mofo");
			  Statement st = con.createStatement();
			  linksRecords=st.executeQuery(selectQuery);
			  while(linksRecords.next())
			  {
				  relatedProperties.add(linksRecords.getString("property"));
			  }
			  //linksRecords.close();
		  }
  		 
		  catch (SQLException s)
		  {
			  Notification.show("SQL statement is not executed! In getting related properties\n"+s.getMessage());
		  }
		  finally
		  {
			  try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		  return relatedProperties;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected void getURIProperties(String subject,String endpoint, Table table)
    {
    	boolean fileCachOk=false;
    	try
    	{
        	String sparqlQuery="select distinct * where { <"+subject+"> ?p  ?o .}";
        /*	if(cachingModels.keySet().contains(Integer.parseInt(task)))
        		fileCachOk = queryModel(sparqlQuery,table);
        	if(fileCachOk == false)
        		Notification.show("The file loading failed");
        	else
        		Notification.show("The file loading succeeded");*/

        	
        	//if the model exists already and it contains the required subject's properties
        	if((cachingModels.keySet().contains(Integer.parseInt(task))) && queryModel(sparqlQuery,table))
        		;//Notification.show("model");
        	else
        	{// the model not exist or exist but subject's properties are not included in it
        		queryEndpoints(sparqlQuery,endpoint,table);
        		//Notification.show("endpoint");
        	}
    	}
    	catch (Exception e)
	 	  {
		        Notification.show("Error in querying "+subject+" properties from cache and endpoint \n"+e.getMessage());
	 	  }
    }
    protected void queryEndpoints(String sparqlQuery,String endpoint,Table table)
    {
    	Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
		com.hp.hpl.jena.query.ResultSet results = qexec.execSelect();
		com.hp.hpl.jena.query.QuerySolution binding=null;
		table.removeAllItems();
	    while (results.hasNext()) 
	    {
	    	binding = results.next();
	    	String property=binding.getResource("?p").toString();
	    	String value;
	    	if(binding.get("?o").isResource())
	    		value=binding.getResource("?o").toString();
	    	else
	    		value=binding.getLiteral("?o").toString();
	    	table.addItem(new Object[] {property,value}, new Integer(table.size()+1));
	    	//String subject=binding.getResource("?s").toString();
	    	////////////////////////addNewResourceToModel(subject,property,value);
	    }
	    qexec.close() ;
    }
    
    /*protected void addNewResourceToModel(String subject,String property,String value)
    {
    	Resource newResource = ResourceFactory.createResource(subject);
    	com.hp.hpl.jena.rdf.model.Property newProperty= ResourceFactory.createProperty(property);
    	if(task.equals("1") && model1 != null)
    		model1.add(newResource, newProperty, value);
    	else if(task.equals("2") && model2 != null)
    		model2.add(newResource, newProperty, value);
    	else if(task.equals("4") && model4 != null)
    		model4.add(newResource, newProperty, value);
    	
    }*/
    protected boolean queryModel(String sparqlQuery,Table table)
    {
 	  Query query = QueryFactory.create(sparqlQuery);
 	  QueryExecution exec=null;
 	  boolean hit=false;
  
 	  exec = QueryExecutionFactory.create(query, cachingModels.get(Integer.parseInt(task)));
 	  com.hp.hpl.jena.query.ResultSet results = null;
 	  results= exec.execSelect();
 	  com.hp.hpl.jena.query.QuerySolution binding=null;
 	  table.removeAllItems();
 	  if(results==null)
 		  return false;
 	  
 	   while(results.hasNext())
 	   {
 		hit=true;
 		binding = results.next();
	    String property=binding.getResource("?p").toString();
	    String value;
	    if(binding.get("?o").isResource())
	    	value=binding.getResource("?o").toString();
	    else
	    	value=binding.getLiteral("?o").toString();
	    table.addItem(new Object[] {property,value}, new Integer(table.size()+1));
	    
 	   }
 	   return hit;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected HashMap<String,String> getEndpoints(String url,String db,String userName,String password)
    {
    	HashMap<String,String> endpoints=null;
    	Connection con = null;
		String driver = "com.mysql.jdbc.Driver";
		
		  try
		  { 
			  Class.forName(driver);
			  con = DriverManager.getConnection(url+db,userName,password);
			  String selectStatement="SELECT *"+
									" FROM Endpoints";
			
			  Statement st = con.createStatement();
			  ResultSet linksRecords=null;
			  linksRecords=st.executeQuery(selectStatement);
			  
			  if(linksRecords!=null)
			  {
				  endpoints=new HashMap<String,String>();
				 
				  while(linksRecords.next())
				  {
					  endpoints.put(linksRecords.getString("ID"),linksRecords.getString("endpoint"));
				  }
			  }
			  else
			  {
				  Notification.show("Warning:","No endpoints retrieved", Type.TRAY_NOTIFICATION);
			  }

			  linksRecords.close();
		  }
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  finally
		  {
			  try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
    	
    	return endpoints;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected List<String> getTaskEndpointsIDs(String url,String db,String userName,String password)
    {
    	List<String> endpoints=null;
    	Connection con = null;
		String driver = "com.mysql.jdbc.Driver";
		
		  try
		  { 
			  Class.forName(driver);
			  con = DriverManager.getConnection(url+db,userName,password);
			  String selectStatement="SELECT sourceEndpoint,destinationEndpoint "+
									" FROM Tasks" +
									" WHERE Tasks.TaskId = "+task;
			
			  Statement st = con.createStatement();
			  ResultSet linksRecords=null;
			  linksRecords=st.executeQuery(selectStatement);
			  
			  if(linksRecords!=null)
			  {
				  endpoints=new ArrayList<String>();
				 
				  while(linksRecords.next())
				  {
					  endpoints.add(linksRecords.getString("sourceEndpoint"));
					  endpoints.add(linksRecords.getString("destinationEndpoint"));
				  }
			  }
			  else
			  {
				  Notification.show("Warning:","No endpoints retrieved", Type.TRAY_NOTIFICATION);
			  }

			  linksRecords.close();
		  }
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  finally
		  {
			  try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
    	
    	return endpoints;
    }
    protected List<String> getTasksInfo(String url,String db,String userName,String password,String usr) throws SQLException
	{
    	Connection con = null;
		String driver = "com.mysql.jdbc.Driver";
		List<String> info=null;
		  try
		  { 
			  Class.forName(driver);
			  con = DriverManager.getConnection(url+db,userName,password);
			  String selectStatement="SELECT TasksUsers.taskId FROM TasksUsers WHERE  TasksUsers.userId = "+usr;
			  Statement st = con.createStatement();
			  ResultSet linksRecords=null;
			  linksRecords=st.executeQuery(selectStatement);
			  if(linksRecords!=null)
			  {
				  info=new ArrayList<String>();
				  while(linksRecords.next())
				  {
					  info.add(linksRecords.getString("taskId"));
				  }
			  }
			  else
			  {
				  Notification.show("Warning:","User has no tasks", Type.TRAY_NOTIFICATION);
			  }

			  linksRecords.close();
		  }
		  catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  finally
		  {
			  try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		return info;
		
	}
    //////////////////////////////////////////////////////////////////////////////////
    protected HashMap<String, String> getLoginInfo(String url,String db,String userName,String password) throws SQLException
	{
    	Connection con = null;
		String driver = "com.mysql.jdbc.Driver";
		HashMap<String, String> info=new HashMap<String, String>();
		  try
		  {
			  Class.forName(driver);
			  con = DriverManager.getConnection(url+db,userName,password);
			  String selectStatement="SELECT userId,userName FROM Users";
			  Statement st = con.createStatement();
			  ResultSet linksRecords=st.executeQuery(selectStatement);
			  while(linksRecords.next())
			  {
				  info.put(linksRecords.getString("userId"),linksRecords.getString("userName"));
			  }
			  linksRecords.close();
		  }
		 catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  finally
		  {
			  try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
		return info;
		
	}
    /////inner class
    public class LinkCandidate 
    {
    	public String source,destination,relation,decision,time;
    	public LinkCandidate(String s, String d, String r,String de, String t)
    	{
    		source=s;
    		destination=d;
    		relation=r;
    		decision=de;
    		time=t;
    	}
    	@Override
    	public String toString() 
    	{
    		String info= source+":"+ destination+":"+relation+":"+decision+":"+time;
    		return info;
    	}
    }
}


