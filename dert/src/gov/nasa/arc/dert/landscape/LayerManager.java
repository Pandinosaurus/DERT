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

package gov.nasa.arc.dert.landscape;

import gov.nasa.arc.dert.landscape.io.TileSource;
import gov.nasa.arc.dert.landscape.layer.DerivativeLayer;
import gov.nasa.arc.dert.landscape.layer.FieldCameraLayer;
import gov.nasa.arc.dert.landscape.layer.FieldLayer;
import gov.nasa.arc.dert.landscape.layer.Layer;
import gov.nasa.arc.dert.landscape.layer.LayerInfo;
import gov.nasa.arc.dert.landscape.layer.RasterLayer;
import gov.nasa.arc.dert.landscape.layer.DerivativeLayer.DerivativeType;
import gov.nasa.arc.dert.landscape.layer.LayerInfo.LayerType;
import gov.nasa.arc.dert.render.LayerEffects;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.util.ColorMap;
import gov.nasa.arc.dert.util.StateUtil;
import gov.nasa.arc.dert.util.StringUtil;
import gov.nasa.arc.dert.view.ColorBarPanel;
import gov.nasa.arc.dert.view.Console;
import gov.nasa.arc.dert.view.surfaceandlayers.SurfaceAndLayersView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Renderer;

/**
 * Helper class to aid Landscape with layers.
 *
 */
public class LayerManager {

	public static int NUM_LAYERS = 7;
	public static final ReadOnlyColorRGBA DEFAULT_GRID_COLOR = ColorRGBA.WHITE;

	// Flags accessed by Landscape
	protected boolean layersEnabled;
	protected boolean shadingFromSurface;
	protected boolean autoAdjustOpacity;
	protected boolean noLayersSelected;

	// Surface grid layer fields
	private ReadOnlyColorRGBA gridColor;
	private double gridCellSize;
	private boolean gridEnabled;

	// Information about available and visible image layers
	private transient Vector<LayerInfo> availableLayers;
	private transient Vector<LayerInfo> visibleLayers;

	// Object to do special layer effects in shader
	protected transient LayerEffects layerEffects;

	// Source of data for layers
	protected transient TileSource source;

	// Base layer
	protected transient RasterLayer baseLayer;

	// Array of layers by texture unit (element set to null if not used).
	protected transient Layer[] layers;

	/**
	 * Constructor
	 */
	public LayerManager() {
		shadingFromSurface = false;
		autoAdjustOpacity = true;
		layersEnabled = true;
		gridColor = DEFAULT_GRID_COLOR;
		gridCellSize = 0;
	}

	/**
	 * Constructor from hash map.
	 */
	public LayerManager(HashMap<String,Object> map) {
		shadingFromSurface = StateUtil.getBoolean(map, "ShadingFromSurface", false);
		autoAdjustOpacity = StateUtil.getBoolean(map, "AutoAdjustOpacity", true);
		layersEnabled = StateUtil.getBoolean(map, "LayersEnabled", true);
		gridColor = StateUtil.getColorRGBA(map, "GridColor", DEFAULT_GRID_COLOR);
		gridCellSize = StateUtil.getDouble(map, "GridCellSize", 0);
		gridEnabled = StateUtil.getBoolean(map, "GridEnabled", false);
		int n = StateUtil.getInteger(map, "LayerInfoCount", 0);
		visibleLayers = new Vector<LayerInfo>();
		visibleLayers.setSize(NUM_LAYERS);
		for (int i=0; i<NUM_LAYERS; ++i) {
			HashMap<String,Object> liMap = (HashMap<String,Object>)map.get("LayerInfo"+i);
			visibleLayers.set(i, new LayerInfo(liMap));
		}
		availableLayers = new Vector<LayerInfo>();
		for (int i=NUM_LAYERS; i<n; ++i) {
			HashMap<String,Object> liMap = (HashMap<String,Object>)map.get("LayerInfo"+i);
			availableLayers.add(new LayerInfo(liMap));
		}
	}

	/**
	 * Initialize this LayerManager with a layer source.
	 * 
	 * @param source
	 * @return
	 */
	public boolean initialize(TileSource source) {
		this.source = source;
		LayerInfo baseLayerInfo = null;
		
		// Get a list of the known layers from the landscape directory
		String[][] sourceLayerInfo = source.getLayerInfo();
		LayerInfo[] knownLayers = new LayerInfo[sourceLayerInfo.length];
		for (int i=0; i<knownLayers.length; ++i) {
			knownLayers[i] = new LayerInfo(sourceLayerInfo[i][0], sourceLayerInfo[i][1], -1);
			if (knownLayers[i].type == LayerType.field)
				knownLayers[i].colorMapName = FieldLayer.defaultColorMapName;
		}
		
		// Base layer (DEM)
		for (int i=0; i<knownLayers.length; ++i)
			if (knownLayers[i].type == LayerType.elevation) {
				baseLayerInfo = knownLayers[i];
				break;
			}
		if (baseLayerInfo == null) {
			Console.println("Elevation layer not found.");
			return (false);
		}
		Properties properties = source.getProperties("elevation");
		if (properties == null) {
			Console.println("Elevation layer properties not found.");
			return (false);
		}
		// elevation min and max are single values, not arrays
		baseLayerInfo.minimum = StringUtil.getDoubleValue(properties, "MinimumValue", false, 0, true);
		baseLayerInfo.maximum = StringUtil.getDoubleValue(properties, "MaximumValue", false, 0, true);

		// first time
		if (availableLayers == null) {
			availableLayers = new Vector<LayerInfo>();
			// Set the list of available layers from the known layers
			for (int i = 0; i < knownLayers.length; ++i) {
				// skip the base layer
				if (knownLayers[i] != baseLayerInfo)
					availableLayers.add(knownLayers[i]);
			}
			
			// Add the derivatives to the list
			LayerInfo lInfo = new LayerInfo("Slope Map", "derivative", DerivativeLayer.defaultColorMapName, 0, 90, false);
			availableLayers.add(lInfo);
			lInfo = new LayerInfo("Aspect Map", "derivative", DerivativeLayer.defaultColorMapName, 0, 360, false);
			availableLayers.add(lInfo);
			lInfo = new LayerInfo("Elevation Map", "derivative", DerivativeLayer.defaultColorMapName,
					baseLayerInfo.minimum, baseLayerInfo.maximum, false);
			availableLayers.add(lInfo);
			lInfo = new LayerInfo("Distance Map", "derivative", DerivativeLayer.defaultColorMapName,
					0, 1, false);
			availableLayers.add(lInfo);
		}
		// otherwise check available layers against reality
		else {
			for (int i=availableLayers.size()-1; i>=0; --i) {
				LayerInfo li = availableLayers.get(i);
				if ((li.type == LayerType.grayimage) || (li.type == LayerType.colorimage) || (li.type == LayerType.field)) {
					boolean foundLayer = false;
					for (int j=0; j<knownLayers.length; ++j) {
						if ((li.type == knownLayers[j].type) && (li.name.equals(knownLayers[j].name))) {
							foundLayer = true;
							break;
						}
					}
					if (!foundLayer)
						availableLayers.remove(i);
				}
			}
			for (int i=0; i<knownLayers.length; ++i) {
				if (knownLayers[i] == baseLayerInfo)
					continue;
				boolean foundLayer = false;
				for (int j=0; j<availableLayers.size(); ++j) {
					LayerInfo li = availableLayers.get(j);
					if ((li.type == knownLayers[i].type) && (li.name.equals(knownLayers[i].name))) {
						foundLayer = true;
						break;
					}
				}
				if (visibleLayers != null) {
					for (int j=0; j<visibleLayers.size(); ++j) {
						LayerInfo li = visibleLayers.get(j);
						if ((li.type == knownLayers[i].type) && (li.name.equals(knownLayers[i].name))) {
							foundLayer = true;
							break;
						}
					}
				}
				if (!foundLayer)
					availableLayers.add(knownLayers[i]);
			}
		}
		Collections.sort(availableLayers);

		// set up the visible layers
		if (visibleLayers == null) {
			visibleLayers = new Vector<LayerInfo>(NUM_LAYERS);
			visibleLayers.setSize(NUM_LAYERS);
			// first time - choose the first image layer found as the default
			for (int i = 0; i < availableLayers.size(); ++i) {
				if ((availableLayers.get(i).type == LayerType.colorimage)
					|| (availableLayers.get(i).type == LayerType.grayimage)) {
					LayerInfo li = availableLayers.get(i);
					visibleLayers.set(0, li);
					availableLayers.remove(i);
					li.layerNumber = 0;
					li.autoblend = false;
					break;
				}
			}
			// set all non-assigned layers to "none"
			for (int i = 0; i < visibleLayers.size(); ++i) {
				if (visibleLayers.get(i) == null)
					visibleLayers.set(i, new LayerInfo("None", "none", i));
			}
		}
		else {
			for (int i=0; i<visibleLayers.size(); ++i) {
				LayerInfo li = visibleLayers.get(i);
				if ((li.type == LayerType.grayimage) || (li.type == LayerType.colorimage) || (li.type == LayerType.field)) {
					boolean foundLayer = false;
					for (int j=0; j<knownLayers.length; ++j)
						if ((li.type == knownLayers[j].type) && (li.name.equals(knownLayers[j].name))) {
							foundLayer = true;
							break;
						}
					if (!foundLayer)
						visibleLayers.set(i, new LayerInfo("None", "none", i));
				}
			}
		}

		// create the layers
		if (layers == null) {
			layers = new Layer[NUM_LAYERS];
		}
		if (!createBaseLayer(baseLayerInfo)) {
			return (false);
		}
		createLayers();

		return (true);
	}

	private boolean createBaseLayer(LayerInfo baseLayerInfo) {
		// already been here
		if (baseLayer != null)
			return (true);

		// create the base layer
		baseLayer = (RasterLayer) createLayer(baseLayerInfo, source, -1);
		if (baseLayer == null) {
			Console.println("Unable to create base layer");
			return (false);
		}
		return (true);
	}

	/**
	 * Get a copy of the layer information list
	 * 
	 * @return
	 */
	public Vector<LayerInfo> getAvailableLayers() {
		return (availableLayers);
	}

	/**
	 * Get the list of selected layers.
	 * 
	 * @return
	 */
	public Vector<LayerInfo> getVisibleLayers() {
		return (visibleLayers);
	}

	/**
	 * Set the list of selected layers.
	 * 
	 * @param selectedLayerInfo
	 */
	public void setLayerSelection(Vector<LayerInfo> visibleLayers, Vector<LayerInfo> availableLayers) {
		this.visibleLayers.clear();
		this.visibleLayers.addAll(visibleLayers);
		this.availableLayers.clear();
		this.availableLayers.addAll(availableLayers);
	}

	/**
	 * Get the base (elevation) layer
	 * 
	 * @return
	 */
	public RasterLayer getBaseLayer() {
		return (baseLayer);
	}

	/**
	 * Get the non-base layers
	 * 
	 * @return
	 */
	public Layer[] getLayers() {
		return (layers);
	}

	/**
	 * Remove field camera layers.
	 * 
	 * @param fieldCamera
	 * @return
	 */
	public boolean removeFieldCamera(String cameraName) {
		boolean found = false;
		for (int i = 0; i < layers.length; ++i) {
			if (layers[i] instanceof FieldCameraLayer) {
				FieldCameraLayer layer = (FieldCameraLayer) layers[i];
				if (cameraName.equals(layer.getLayerName())) {
					layer.dispose();
					layers[i] = null;
					visibleLayers.set(i, new LayerInfo("None", "none", i));
					layerEffects = new LayerEffects(layers, layerEffects);
					layerEffects.setEnabled(true);
					found = true;
				}
			}
		}
		Iterator<LayerInfo> iterator = availableLayers.iterator();
		while (iterator.hasNext()) {
			LayerInfo li = iterator.next();
			if (li.name.equals(cameraName) && ((li.type == LayerType.footprint) || (li.type == LayerType.viewshed))) {
				iterator.remove();
			}
		}
		if (found) {
			SurfaceAndLayersView view = ConfigurationManager.getInstance().getCurrentConfiguration().getSurfaceAndLayersView();
			if (view != null) {
				view.updateVisibleLayers();
			}
		}
		return (found);
	}

	/**
	 * Add field camera layers
	 * 
	 * @param fieldCamera
	 */
	public void addFieldCamera(String cameraName) {
		availableLayers.add(new LayerInfo(cameraName, "footprint", -1));
		availableLayers.add(new LayerInfo(cameraName, "viewshed", -1));
	}

	private void createLayers() {
		Layer[] newList = new Layer[NUM_LAYERS];
		noLayersSelected = true;
		for (int i = 0; i < visibleLayers.size(); ++i) {
			if (visibleLayers.get(i).type == LayerType.none) {
				newList[i] = null;
			} else {
				// see if we have this layer already and move it to the new slot
				boolean found = false;
				for (int j = 0; j < layers.length; ++j) {
					if (layers[j] == null) {
						continue;
					}
					if (visibleLayers.get(i).name.equals(layers[j].toString()) && (visibleLayers.get(i).type == layers[j].getLayerType())) {
						layers[j].setBlendFactor(visibleLayers.get(i).opacity*visibleLayers.get(i).show);
						newList[i] = layers[j];
						layers[j] = null;
						found = true;
						break;
					}
				}
				// if not, create the layer
				if (!found) {
					newList[i] = createLayer(visibleLayers.get(i), source, i);
					newList[i].setBlendFactor(visibleLayers.get(i).opacity*visibleLayers.get(i).show);
				}
				noLayersSelected = false;
			}
		}
		// dispose of the layers we didn't reuse
		for (int i = 0; i < layers.length; ++i) {
			if (layers[i] != null) {
				layers[i].dispose();
				layers[i] = null;
			}
		}
		layers = newList;
		
		if (noLayersSelected)
			shadingFromSurface = true;

		// create the layer effects shaders
		layerEffects = new LayerEffects(layers, layerEffects);
		enableLayers(layersEnabled);
		enableGrid(gridEnabled);
		layerEffects.setEnabled(true);
		ColorBarPanel.resetColorBars();
	}

	private Layer createLayer(LayerInfo layerInfo, TileSource source, int index) {
		try {
			if (layerInfo.type == LayerType.none) {
				return (null);
			} else if (layerInfo.type == LayerType.derivative) {
				if (layerInfo.name.contains("Elevation")) {
					return (new DerivativeLayer(DerivativeType.Elevation, layerInfo, baseLayer));
				} else if (layerInfo.name.contains("Slope")) {
					return (new DerivativeLayer(DerivativeType.Slope, layerInfo, baseLayer));
				} else if (layerInfo.name.contains("Aspect")) {
					return (new DerivativeLayer(DerivativeType.Aspect, layerInfo, baseLayer));
				} else if (layerInfo.name.contains("Distance")) {
					return (new DerivativeLayer(DerivativeType.Distance, layerInfo, baseLayer));
				} else {
					return (null);
				}
			} else if ((layerInfo.type == LayerType.footprint) || (layerInfo.type == LayerType.viewshed)) {
				return (new FieldCameraLayer(layerInfo, index));
			} else if (layerInfo.type == LayerType.field) {
				return (new FieldLayer(layerInfo, source));
			} else {
				return (new RasterLayer(layerInfo, source));
			}
		} catch (Exception e) {
			Console.println("Unable to create layer " + layerInfo.name + ", see log.");
			e.printStackTrace();
			return (null);
		}
	}

	/**
	 * Get the LayerEffects object
	 * 
	 * @return
	 */
	public LayerEffects getLayerEffects() {
		return (layerEffects);
	}

	/**
	 * Show layers
	 * 
	 * @param enable
	 */
	public void enableLayers(boolean enable) {
		layerEffects.layersEnabled = enable && !noLayersSelected;
		layersEnabled = enable;
	}

	/**
	 * Show shadows
	 * 
	 * @param enable
	 */
	public void enableShadow(boolean enable) {
		layerEffects.shadowEnabled = enable;
	}
	
	public void setAllDark(boolean allDark) {
		layerEffects.allDark = allDark;
	}

	/**
	 * Set the blend factor for a layer indicated by the index
	 * 
	 * @param index
	 * @param value
	 */
	public void setLayerBlendFactor(int index, float value) {
		layerEffects.blendFactor[index] = value*visibleLayers.get(index).show;
		visibleLayers.get(index).opacity = value;
	}

	/**
	 * Show the surface grid
	 * 
	 * @param enable
	 */
	public void enableGrid(boolean enable) {
		gridEnabled = enable;
		layerEffects.gridCell = gridCellSize;
		layerEffects.gridColor = gridColor.toArray(null);
		layerEffects.gridEnabled = enable;
	}

	/**
	 * Is the surface grid showing?
	 * 
	 * @return
	 */
	public boolean isGridEnabled() {
		return (gridEnabled);
	}

	/**
	 * Set the surface grid color
	 * 
	 * @param color
	 */
	public void setGridColor(ReadOnlyColorRGBA color) {
		gridColor = color;
		layerEffects.gridColor = color.toArray(null);
	}

	/**
	 * Get the surface grid color
	 * 
	 * @return
	 */
	public ReadOnlyColorRGBA getGridColor() {
		return (gridColor);
	}

	/**
	 * Set the surface grid cell size
	 * 
	 * @param size
	 */
	public void setGridCellSize(double size) {
		gridCellSize = size;
		layerEffects.gridCell = size;
	}

	/**
	 * Get the surface grid cell size
	 * 
	 * @return
	 */
	public double getGridCellSize() {
		return (gridCellSize);
	}

	/**
	 * Get a property from the base layer. If not available, try the other
	 * layers
	 * 
	 * @param key
	 * @return
	 */
	public Object getLayerProperty(String key) {
		for (int i = 0; i < layers.length; ++i) {
			if (layers[i] != null) {
				Object prop = layers[i].getProperties().getProperty(key);
				if (prop != null) {
					return (prop);
				}
			}
		}
		return (null);
	}

	/**
	 * Get the color maps used by the derivative and field layers
	 * 
	 * @return
	 */
	public ArrayList<ColorMap> getColorMaps() {
		ArrayList<ColorMap> list = new ArrayList<ColorMap>();
		for (int i = 0; i < layers.length; ++i) {
			if (layers[i] == null)
				continue;
			if (layers[i] instanceof FieldLayer)
				list.add(((FieldLayer) layers[i]).getColorMap());
			else if (layers[i] instanceof DerivativeLayer)
				list.add(((DerivativeLayer) layers[i]).getColorMap());
		}
		return (list);
	}

	/**
	 * Pre-render layers
	 * 
	 * @param renderer
	 */
	public void renderLayers(Renderer renderer) {
		for (int i = 0; i < layers.length; ++i) {
			if (layers[i] != null) {
				layers[i].prerender(renderer);
			}
		}
	}

	/**
	 * Prepare layer info objects for persistence
	 */
	public HashMap<String,Object> saveAsHashMap() {
		HashMap<String,Object> map = new HashMap<String,Object>();
		map.put("ShadingFromSurface", new Boolean(shadingFromSurface));
		map.put("AutoAdjustOpacity", new Boolean(autoAdjustOpacity));
		map.put("LayersEnabled", new Boolean(layersEnabled));
		StateUtil.putColorRGBA(map, "GridColor", gridColor);
		map.put("GridCellSize", new Double(gridCellSize));
		map.put("GridEnabled", new Boolean(gridEnabled));
		map.put("LayerInfoCount", new Integer(visibleLayers.size()+availableLayers.size()));
		for (int i = 0; i < visibleLayers.size(); ++i)
			map.put("LayerInfo"+i, visibleLayers.get(i).getAsHashMap());
		for (int i=0; i<availableLayers.size(); ++i)
			map.put("LayerInfo"+(i+NUM_LAYERS), availableLayers.get(i).getAsHashMap());
		return(map);
	}
}
