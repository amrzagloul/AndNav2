// Created by plusminus on 20:50:06 - 03.10.2008
package org.andnav2.osm.views.overlay;

import java.util.List;

import org.andnav2.R;
import org.andnav2.osm.views.OSMMapView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;


public abstract 
class OSMMapViewItemizedOverlayWithFocus<T extends OSMMapViewOverlayItem> 
extends OSMMapViewItemizedOverlay<T> 
{
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int DESCRIPTION_BOX_PADDING = 3;
	public static final int DESCRIPTION_BOX_CORNERWIDTH = 3;

	public static final int DESCRIPTION_LINE_HEIGHT = 12;
	/** Additional to <code>DESCRIPTION_LINE_HEIGHT</code>. */
	public static final int DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = 2;


	protected static final Point DEFAULTMARKER_FOCUSED_HOTSPOT = new Point(10, 19);
	protected static final int DEFAULTMARKER_BACKGROUNDCOLOR = Color.rgb(101, 185, 200);

	protected static final int DESCRIPTION_MAXWIDTH = 200;

	// ===========================================================
	// Fields
	// ===========================================================

	private OSMMapViewMarkerForFocus mMarkerFocusedBase;

	private final int mMarkerFocusedBackgroundColor;
	private final int mMarkerFocusedWidth, mMarkerFocusedHeight;
	private final Paint mMarkerBackgroundPaint, mDescriptionPaint, mTitlePaint;

	
	private T mFocusedItem;
	private boolean mFocusItemsOnTap = true;

	private final String UNKNOWN;

	protected int mPaddingTitleLeft = 10;
	private final int mPaddingTitleRight = 0;
	protected boolean mDrawBaseIntemUnderFocusedItem = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OSMMapViewItemizedOverlayWithFocus(
			final Context ctx, 
			final OnItemTapListener<T> aOnItemTapListener) 
	{
		this(ctx, null, null, aOnItemTapListener);
	}

	public OSMMapViewItemizedOverlayWithFocus(
			final Context ctx, 
			final OSMMapViewMarker pMarker, 
			final OSMMapViewMarkerForFocus pMarkerFocusedBase, 
			final OnItemTapListener<T> aOnItemTapListener)
	{
		super(ctx, pMarker, aOnItemTapListener);

		this.UNKNOWN = ctx.getString(R.string.unknown);

		this.mMarkerFocusedBase = (pMarkerFocusedBase != null)
					? pMarkerFocusedBase
					: new OSMMapViewMarkerForFocus(
							ctx.getResources().getDrawable(R.drawable.marker_default_focused_base),
							DEFAULTMARKER_FOCUSED_HOTSPOT,
							DEFAULTMARKER_BACKGROUNDCOLOR);
		
		this.mMarkerFocusedBackgroundColor = mMarkerFocusedBase.getBackgroundColor();
		
		this.mMarkerBackgroundPaint = new Paint(); // Color is set in onDraw(...)

		this.mDescriptionPaint = new Paint();
		this.mDescriptionPaint.setAntiAlias(true);
		this.mTitlePaint = new Paint();
		this.mTitlePaint.setFakeBoldText(true);
		this.mTitlePaint.setAntiAlias(true);

		this.mMarkerFocusedWidth = this.mMarkerFocusedBase.getIntrinsicWidth();
		this.mMarkerFocusedHeight = this.mMarkerFocusedBase.getIntrinsicHeight();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public void release() {
		super.release();
		this.mMarkerFocusedBase = null;
	}

	public T getFocusedItem() {
		return this.mFocusedItem;
	}

	public void setFocusedItem(final T pItem){
		this.mFocusedItem = pItem;
	}

	public void unSetFocusedItem(){
		this.mFocusedItem = null;
	}

	public void setAutoFocusItemsOnTap(final boolean doit) {
		this.mFocusItemsOnTap = doit;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected boolean onTap(final int pIndex) {
		if(this.mFocusItemsOnTap) {
			this.mFocusedItem = getOverlayItems().get(pIndex);
		}

		return super.onTap(pIndex);
	}

	@Override
	protected void onDrawFinished(final Canvas c, final OSMMapView osmv) {
		if(this.mFocusedItem == null) return;
		
		final List<T> overlayItems = this.getOverlayItems();
		if(overlayItems == null) return;
		
		final Point screenCoords = new Point();

		osmv.getProjection().toPixels(this.mFocusedItem, screenCoords);

		if(this.mDrawBaseIntemUnderFocusedItem) {
			super.onDrawItem(c, 0, screenCoords);
		}
		
		/* Calculate and set the bounds of the marker. */
		final int left = screenCoords.x - this.mMarker.getHotSpot().x;
		final int right = left + this.mMarkerFocusedWidth;
		final int top = screenCoords.y - this.mMarker.getHotSpot().y;
		final int bottom = top + this.mMarkerFocusedHeight;
		this.mMarkerFocusedBase.setBounds(left, top, right, bottom);

		/* Strings of the OverlayItem, we need. */
		final String itemTitle = (this.mFocusedItem.mTitle == null) ? this.UNKNOWN : this.mFocusedItem.mTitle;
		final String itemDescription = (this.mFocusedItem.mDescription == null) ? this.UNKNOWN : this.mFocusedItem.mDescription;

		/* Store the width needed for each char in the description to a float array. This is pretty efficient. */
		final float[] widths = new float[itemDescription.length()];
		this.mDescriptionPaint.getTextWidths(itemDescription, widths);

		final StringBuilder sb = new StringBuilder();
		int maxWidth = 0;
		int curLineWidth = 0;
		int lastStop = 0;
		int i;
		int lastwhitespace = 0;
		/* Loop through the charwidth array and harshly insert a linebreak,
		 * when the width gets bigger than DESCRIPTION_MAXWIDTH. */
		for (i = 0; i < widths.length; i++) {
			final char curChar = itemDescription.charAt(i);
			if(!Character.isLetter(curChar)) {
				lastwhitespace = i;
			}

			final float charwidth = widths[i];

			if(curLineWidth + charwidth > DESCRIPTION_MAXWIDTH || curChar == '\n'){
				if(lastStop == lastwhitespace) {
					i--;
				} else {
					i = lastwhitespace;
				}

				sb.append(itemDescription.subSequence(lastStop, i));
				if(curChar != '\n') {
					sb.append('\n');
				}

				lastStop = i;
				maxWidth = Math.max(maxWidth, curLineWidth);
				curLineWidth = 0;
			}

			curLineWidth += charwidth;
		}
		/* Add the last line to the rest to the buffer. */
		if(i != lastStop){
			final String rest = itemDescription.substring(lastStop, i);

			maxWidth = Math.max(maxWidth, (int)this.mDescriptionPaint.measureText(rest));

			sb.append(rest);
		}
		final String[] lines = sb.toString().split("\n");

		/* The title also needs to be taken into consideration for the width calculation. */
		final int titleWidth = (int)this.mDescriptionPaint.measureText(itemTitle) + this.mPaddingTitleLeft + this.mPaddingTitleRight;

		maxWidth = Math.max(maxWidth, titleWidth);
		final int descWidth = Math.min(maxWidth, DESCRIPTION_MAXWIDTH);

		/* Calculate the bounds of the Description box that needs to be drawn. */
		final int descBoxLeft = left - descWidth / 2 - DESCRIPTION_BOX_PADDING + this.mMarkerFocusedWidth / 2;
		final int descBoxRight = descBoxLeft + descWidth + 2 * DESCRIPTION_BOX_PADDING;
		final int descBoxBottom = top;
		final int descBoxTop = descBoxBottom
			- DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT
			- (lines.length + 1) * DESCRIPTION_LINE_HEIGHT /* +1 because of the title. */
			- 2 * DESCRIPTION_BOX_PADDING;

		/* Twice draw a RoundRect, once in black with 1px as a small border. */
		this.mMarkerBackgroundPaint.setColor(Color.BLACK);
		c.drawRoundRect(new RectF(descBoxLeft - 1, descBoxTop - 1, descBoxRight + 1, descBoxBottom + 1),
				DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH,
				this.mDescriptionPaint);
		this.mMarkerBackgroundPaint.setColor(this.mMarkerFocusedBackgroundColor);
		c.drawRoundRect(new RectF(descBoxLeft, descBoxTop, descBoxRight, descBoxBottom),
				DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH,
				this.mMarkerBackgroundPaint);

		final int descLeft = descBoxLeft + DESCRIPTION_BOX_PADDING;
		int descTextLineBottom = descBoxBottom - DESCRIPTION_BOX_PADDING;

		/* Draw all the lines of the description. */
		for(int j = lines.length - 1; j >= 0; j--){
			c.drawText(lines[j].trim(), descLeft, descTextLineBottom, this.mDescriptionPaint);
			descTextLineBottom -= DESCRIPTION_LINE_HEIGHT;
		}
		/* Draw the title. */
		c.drawText(itemTitle, descLeft + this.mPaddingTitleLeft, descTextLineBottom - DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT, this.mTitlePaint);
		c.drawLine(descBoxLeft, descTextLineBottom, descBoxRight, descTextLineBottom, this.mDescriptionPaint);

		/* Finally draw the marker base. This is done in the end to make it look better. */
		this.mMarkerFocusedBase.draw(c);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
