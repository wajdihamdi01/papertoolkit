package ink {		
	
	import flash.display.Sprite;
	import flash.display.Graphics;
	import flash.events.Event;
	import flash.events.MouseEvent;

	// As a display object, it can be added to an Ink object
	// As a representation of an Ink Stroke, it can be used for calcuation/clustering purposes
	public class InkStroke extends Sprite {

		// static constants
		private static const DEFAULT_COLOR:uint = 0xDADADA;
		private static const HIGHLIGHT_COLOR:uint = 0xFF99AA;

		private var xSamples:Array = new Array();
		private var ySamples:Array = new Array();
		
		private var xMin:Number = Number.MAX_VALUE;
		private var yMin:Number = Number.MAX_VALUE;
		private var xMax:Number = Number.MIN_VALUE;
		private var yMax:Number = Number.MIN_VALUE;

		
		private var g:Graphics = graphics;

		private var strokeWidth:Number = 1.5;
		
		private var color:uint = DEFAULT_COLOR;
		
		// whether this stroke should be highlighted, and rendered using the highlight color
		private var highlight:Boolean = false;

		// timestamps
		private var begin:Number;
		private var end:Number;
		
		// the last sample we received. Helps us for rendering with splines
		private var lastXVal:Number = -1;
		private var lastYVal:Number = -1;
		
		// two samples ago. It allows us to render the strokes more cleanly... using splines
		private var lastLastXVal:Number = -1;
		private var lastLastYVal:Number = -1;

		
		// constructor
		public function InkStroke(beginTS:String="0", endTS:String="0"):void {			
			//trace(beginTS + " to " + endTS);
			begin = parseInt(beginTS);
			end = parseInt(endTS);
			buttonMode = true;
		}		

		// determines whether this stroke should be highlighted on screen
		public function set highlighted(value:Boolean):void {
			highlight=value;
			if (highlight) {
				color = HIGHLIGHT_COLOR;
			} else {
				color = DEFAULT_COLOR;
			}
		}

		public function get beginTimestamp():Number {
			return begin;
		}

		// add an ink sample...
		public function addPoint(xVal:Number, yVal:Number, f:Number):void {
			// assume f is 0 to 128
			// later: cap it to 0 to 128
			
			// obsolete directions: =)
			// vary opacity and width based on the force
			// from about 70 to 100, treat the width the same, but vary opacity
			// from about 0 to 70, vary width
			// from about 100 to 128, vary width
			
			var modifiedStrokeWidth:Number = strokeWidth;
			var delta:Number = 50 - f;
			modifiedStrokeWidth -= delta * 0.02;
			// trace("New Stroke Width: " + modifiedStrokeWidth);
			
			// don't modify the alpha!
			// var modifiedAlpha:Number = .95;
			// modifiedAlpha -= delta * .01;
			
			g.lineStyle(modifiedStrokeWidth, color);
			if (xSamples.length == 0) {
				g.moveTo(xVal, yVal);				
				lastXVal = xVal;
				lastYVal = yVal;
				lastLastXVal = xVal;
				lastLastYVal = yVal;
			}
			else {
				// check to see if it's a large jump
				// in the future, we'll want to subdivide and add new points, so the lineTo looks smoother :)
				var dX:Number = xVal - lastXVal;
				var dY:Number = yVal - lastYVal;
				// trace("InkStroke.as: dX,dY from last sample: " + dX + ", " + dY);
				
				
				// if it's an UBER JUMP... then this may be the problem where anoto notebooks 
				// have noncontiguous pattern. In that case, just disregard this whole point
				if (Math.abs(dX) > 500 || Math.abs(dY) > 500) {
					// trace("InkStroke.as: Disregarding a Large Jump in Ink Samples...");
					return;
				}
				
				
				// reverted to simple linetos..
				g.lineTo(xVal, yVal);
				
				// advance the last values
				lastLastXVal = lastXVal;
				lastLastYVal = lastYVal;
				lastXVal = xVal;
				lastYVal = yVal;
			}
			xSamples.push(xVal);
			ySamples.push(yVal);

			
			
			xMin = Math.min(xMin, xVal);
			xMax = Math.max(xMax, xVal);
			yMin = Math.min(yMin, yVal);
			yMax = Math.max(yMax, yVal);
			
			//trace(xVal + " " + yVal);
		}
		
		public function get minX():Number {
			return xMin;
		}
		public function get minY():Number {
			return yMin;
		}
		public function get maxX():Number {
			return xMax;
		}
		public function get maxY():Number {
			return yMax;
		}
		
		public function get lastX():Number {
			return lastXVal;
		}
		public function get lastY():Number {
			return lastYVal;
		}
		
		
		public function set inkColor(c:uint):void {
			color = c;
		}
		public function set inkWidth(w:Number):void {
			strokeWidth = w;
		}

	}
}