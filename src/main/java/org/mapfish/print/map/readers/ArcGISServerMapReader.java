/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.StringUtils;
import org.pvalsecc.misc.URIUtils;

public class ArcGISServerMapReader extends HTTPMapReader {


    public static class Factory implements MapReaderFactory {
        @Override
        public List<MapReader> create(String type, RenderingContext context,
                PJsonObject params) {
            ArrayList<MapReader> target = new ArrayList<MapReader>();
            PJsonArray layers = params.getJSONArray("layers");
            for (int i = 0; i < layers.size(); i++) {
                String layer = layers.getString(i);
                target.add(new ArcGISServerMapReader(layer, context, params));
            }
            return target;
        }

    }

    private final String format;
    protected final List<String> layers = new ArrayList<String>();

    private ArcGISServerMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        layers.add(layer);
        format = params.getString("format");
    }

    protected void renderTiles(TileRenderer formatter, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException {
        //tiling not supported and not really needed (tilecache doesn't support this protocol) for ArcGIS Server protocol...
        List<URI> uris = new ArrayList<URI>(1);
        uris.add(commonUri);
        formatter.render(transformer, uris, parallelMapTileLoader, context, opacity, 1, 0, 0,
                transformer.getRotatedBitmapW(), transformer.getRotatedBitmapH());
    }

    protected TileRenderer.Format getFormat() {
        if (format.equals("image/svg+xml")) {
            return TileRenderer.Format.SVG;
        } else if (format.equals("application/x-pdf")) {
            return TileRenderer.Format.PDF;
        } else {
            return TileRenderer.Format.BITMAP;
        }
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        final String t_srs;
        // Use mapserver rotation
        URIUtils.addParamOverride(result, "map_angle", String.valueOf(-Math.toDegrees(transformer.getRotation())));
        transformer.setRotation(0);

        final long w;
        final long h;
        if (format.equals("image/svg+xml")) {
            URIUtils.addParamOverride(result, "FORMAT", "svg");
            w = transformer.getRotatedSvgW();
            h = transformer.getRotatedSvgH();
        } else if (format.equals("application/x-pdf")) {
            URIUtils.addParamOverride(result, "FORMAT", "pdf");
            w = transformer.getRotatedBitmapW();
            h = transformer.getRotatedBitmapH();
        } else {
            URIUtils.addParamOverride(result, "FORMAT", "png");
            w = transformer.getRotatedBitmapW();
            h = transformer.getRotatedBitmapH();
        }
        t_srs = "102113";
        URIUtils.addParamOverride(result, "LAYERS", StringUtils.join(layers, " "));
        URIUtils.addParamOverride(result, "F", "image");
        URIUtils.addParamOverride(result, "BBOXSR", t_srs);
        URIUtils.addParamOverride(result," IMAGESR",String.format("%s",t_srs));
        URIUtils.addParamOverride(result, "SIZE", String.format("%d %d", w, h));
        URIUtils.addParamOverride(result, "BBOX", String.format("%s %s %s %s", transformer.getRotatedMinGeoX(), transformer.getRotatedMinGeoY(), transformer.getRotatedMaxGeoX(), transformer.getRotatedMaxGeoY()));
        URIUtils.addParamOverride(result, "DPI", String.valueOf(transformer.getDpi()));
        if (!first) {
            URIUtils.addParamOverride(result, "TRANSPARENT", "true");
        }
    }

    public boolean testMerge(MapReader other) {
        if (canMerge(other)) {
            ArcGISServerMapReader ms = (ArcGISServerMapReader) other;
            layers.addAll(ms.layers);
            return true;
        }
        return false;
    }

    public boolean canMerge(MapReader other) {
        if (!super.canMerge(other)) {
            return false;
        }

        if (other instanceof ArcGISServerMapReader) {
            ArcGISServerMapReader wms = (ArcGISServerMapReader) other;
            return format.equals(wms.format);
        } else {
            return false;
        }
    }

    public String toString() {
        return StringUtils.join(layers, ", ");
    }

}