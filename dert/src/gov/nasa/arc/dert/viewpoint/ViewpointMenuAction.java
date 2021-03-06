/**

DERT is a viewer for digital terrain models created from data collected during NASA missions.

DERT is Released in under the NASA Open Source Agreement (NOSA) found in the “LICENSE” folder where you
downloaded DERT.

DERT includes 3rd Party software. The complete copyright notice listing for DERT is:

Copyright © 2015 United States Government as represented by the Administrator of the National Aeronautics and
Space Administration.  No copyright is claimed in the United States under Title 17, U.S.Code. All Other Rights
Reserved.

Desktop Exploration of Remote Terrain (DERT) could not have been written without the aid of a number of free,
open source libraries. These libraries and their notices are listed below. Find the complete third party license
listings in the separate “DERT Third Party Licenses” pdf document found where you downloaded DERT in the
LICENSE folder.
 
JogAmp Ardor3D Continuation
Copyright © 2008-2012 Ardor Labs, Inc.
 
JogAmp
Copyright 2010 JogAmp Community. All rights reserved.
 
JOGL Portions Sun Microsystems
Copyright © 2003-2009 Sun Microsystems, Inc. All Rights Reserved.
 
JOGL Portions Silicon Graphics
Copyright © 1991-2000 Silicon Graphics, Inc.
 
Light Weight Java Gaming Library Project (LWJGL)
Copyright © 2002-2004 LWJGL Project All rights reserved.
 
Tile Rendering Library - Brian Paul 
Copyright © 1997-2005 Brian Paul. All Rights Reserved.
 
OpenKODE, EGL, OpenGL , OpenGL ES1 & ES2
Copyright © 2007-2010 The Khronos Group Inc.
 
Cg
Copyright © 2002, NVIDIA Corporation
 
Typecast - David Schweinsberg 
Copyright © 1999-2003 The Apache Software Foundation. All rights reserved.
 
PNGJ - Herman J. Gonzalez and Shawn Hartsock
Copyright © 2004 The Apache Software Foundation. All rights reserved.
 
Apache Harmony - Open Source Java SE
Copyright © 2006, 2010 The Apache Software Foundation.
 
Guava
Copyright © 2010 The Guava Authors
 
GlueGen Portions
Copyright © 2010 JogAmp Community. All rights reserved.
 
GlueGen Portions - Sun Microsystems
Copyright © 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 
SPICE
Copyright © 2003, California Institute of Technology.
U.S. Government sponsorship acknowledged.
 
LibTIFF
Copyright © 1988-1997 Sam Leffler
Copyright © 1991-1997 Silicon Graphics, Inc.
 
PROJ.4
Copyright © 2000, Frank Warmerdam

LibJPEG - Independent JPEG Group
Copyright © 1991-2018, Thomas G. Lane, Guido Vollbeding
 

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY KIND,
EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
THAT THE SUBJECT SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY
WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE. THIS AGREEMENT
DOES NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY
PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR
ANY OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.  FURTHER,
GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES REGARDING THIRD-PARTY
SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE, AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity:  RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS AGAINST THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR
RECIPIENT.  IF RECIPIENT'S USE OF THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES,
DEMANDS, DAMAGES, EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES
FROM PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT SOFTWARE,
RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED STATES GOVERNMENT, ITS
CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE EXTENT
PERMITTED BY LAW.  RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE,
UNILATERAL TERMINATION OF THIS AGREEMENT.

**/

package gov.nasa.arc.dert.viewpoint;

import gov.nasa.arc.dert.Dert;
import gov.nasa.arc.dert.action.CheckBoxMenuItemAction;
import gov.nasa.arc.dert.action.MenuItemAction;
import gov.nasa.arc.dert.action.PopupMenuAction;
import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.state.AnimationState;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.state.ViewpointState;
import gov.nasa.arc.dert.viewpoint.Viewpoint.ViewpointMode;

import java.awt.PopupMenu;
import java.awt.Toolkit;

import javax.swing.ImageIcon;

/**
 * Activates hike mode.
 *
 */
public class ViewpointMenuAction extends PopupMenuAction {
	
	protected static ViewpointMenuAction INSTANCE;
	
	public static ImageIcon vpIcon, vpHikeIcon, vpMapIcon, bootIcon, mapIcon;
	
	static {
		vpIcon = Icons.getImageIcon("viewpoint.png");
		vpHikeIcon = Icons.getImageIcon("viewpointonfoot.png");
		vpMapIcon = Icons.getImageIcon("viewpointmap.png");
		bootIcon = Icons.getImageIcon("boot.png");
		mapIcon = Icons.getImageIcon("map.png");
	}

	protected boolean oldOnTop;
	protected CheckBoxMenuItemAction nominal, map, hike;
	
	public static ViewpointMenuAction getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ViewpointMenuAction();
		return(INSTANCE);
	}

	/**
	 * Constructor
	 */
	protected ViewpointMenuAction() {
		super("set viewpoint mode", null, vpIcon);

	}

	@Override
	protected void fillMenu(PopupMenu menu) {
		ViewpointController controller = Dert.getWorldView().getScenePanel().getViewpointController();
		ViewpointMode currentMode = controller.getViewpoint().getMode();
		nominal = new CheckBoxMenuItemAction("Model-centric") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Nominal);
				map.setState(false);
				hike.setState(false);
			}
		};
		nominal.setState(currentMode == ViewpointMode.Nominal);
		menu.add(nominal);
		map = new CheckBoxMenuItemAction("Map") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Map);
				nominal.setState(false);
				hike.setState(false);
			}
		};
		map.setState(currentMode == ViewpointMode.Map);
		menu.add(map);
		hike = new CheckBoxMenuItemAction("First-person") {			
			@Override
			protected void run() {
				setMode(ViewpointMode.Hike);
				nominal.setState(false);
				map.setState(false);
			}
		};
		hike.setState(currentMode == ViewpointMode.Hike);
		menu.add(hike);
		
		menu.addSeparator();

		// Open the viewpoint view.
		MenuItemAction viewpointListAction = new MenuItemAction("Open Viewpoint List") {
			@Override
			public void run() {
				ViewpointState vpState = (ViewpointState)ConfigurationManager.getInstance().getCurrentConfiguration().getState("ViewpointState");
				vpState.open(true);
			}
		};
		menu.add(viewpointListAction);
		// Open the animation view.
		MenuItemAction animationAction = new MenuItemAction("Open Animation Control Panel") {
			@Override
			public void run() {
				AnimationState aState = (AnimationState)ConfigurationManager.getInstance().getCurrentConfiguration().getState("AnimationState");
				aState.open(true);
			}
		};
		menu.add(animationAction);
	}
	
	protected void setMode(ViewpointMode mode) {
		ViewpointController controller = Dert.getWorldView().getScenePanel().getViewpointController();
		ViewpointMode currentMode = controller.getViewpoint().getMode();
		if (currentMode == mode)
			return;
		if (!Dert.getWorldView().getViewpoint().setMode(mode)) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		switch (mode) {
		case Nominal:
			World.getInstance().setMapElementsOnTop(oldOnTop);
			break;
		case Hike:
			World.getInstance().setMapElementsOnTop(oldOnTop);
			controller.updateCoR();
			break;
		case Map:
			oldOnTop = World.getInstance().isMapElementsOnTop();
			World.getInstance().setMapElementsOnTop(true);
			break;
		}
		setModeIcon(mode);
	}
	
	protected void setModeIcon(ViewpointMode mode) {
		switch (mode) {
		case Nominal:
			setIcon(vpIcon);
			break;
		case Hike:
			setIcon(vpHikeIcon);
			break;
		case Map:
			setIcon(vpMapIcon);
			break;
		}
	}

}
