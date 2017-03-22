package zemfi.de.vertaktoid.mei;

import java.util.Comparator;

import nu.xom.Element;
import zemfi.de.vertaktoid.model.Measure;

class MeasureElementPair {
    private Measure measure;
    private Element element;

    MeasureElementPair(Measure measure, Element element) {
        this.measure = measure;
        this.element = element;
    }

    Measure getMeasure() {
        return measure;
    }

    void setMeasure(Measure measure) {
        this.measure = measure;
    }

    Element getElement() {
        return element;
    }

    void setElement(Element element) {
        this.element = element;
    }

    final static Comparator<MeasureElementPair> MEASURE_ELEMENT_PAIR_COMPARATOR
            = new Comparator<MeasureElementPair>() {
        @Override
        public int compare(MeasureElementPair e1, MeasureElementPair e2) {
            return e1.getMeasure().sequenceNumber - e2.getMeasure().sequenceNumber;
        }
    };
}
