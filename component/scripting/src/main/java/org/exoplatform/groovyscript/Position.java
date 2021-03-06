/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.groovyscript;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Position {

    /** . */
    private final int col;

    /** . */
    private final int line;

    public Position(int col, int line) {
        if (col < 0) {
            throw new IllegalArgumentException();
        }
        if (line < 0) {
            throw new IllegalArgumentException();
        }

        //
        this.col = col;
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Position) {
            Position that = (Position) obj;
            return col == that.col && line == that.line;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Position[col=" + col + ",line=" + line + "]";
    }
}
