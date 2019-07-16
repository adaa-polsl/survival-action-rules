package pl.polsl.survival.actionrules;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import adaa.analytics.rules.logic.representation.Action;
import adaa.analytics.rules.logic.representation.AnyValueSet;
import adaa.analytics.rules.logic.representation.ElementaryCondition;
import adaa.analytics.rules.logic.representation.Interval;

public class ActionIntersectionTest {
	@Test
	public void intersect_normalActions() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(4.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));
		Action outputAction = new Action(
				new ElementaryCondition("a", new Interval(8.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));

		// When
		ElementaryCondition intersected =  action1.intersect(action2);
		Action intersectedAction = (Action)intersected;

		// Then
		assertEquals(outputAction.getLeftValue(), intersectedAction.getLeftValue());
		assertEquals(outputAction.getRightValue(), intersectedAction.getRightValue());
	}

	@Test
	public void intersect_oneSteadyAction() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));
		Action outputAction = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));

		// When
		ElementaryCondition intersected =  action1.intersect(action2);
		Action intersectedAction = (Action)intersected;

		// Then
		assertEquals(outputAction.getLeftValue(), intersectedAction.getLeftValue());
		assertEquals(outputAction.getRightValue(), intersectedAction.getRightValue());
	}

	@Test
	public void intersect_oneSteadyActionReversed() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));
		Action outputAction = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));

		// When
		ElementaryCondition intersected =  action2.intersect(action1); // action nb 2 makes intersection
		Action intersectedAction = (Action)intersected;

		// Then
		assertEquals(outputAction.getLeftValue(), intersectedAction.getLeftValue());
		assertEquals(outputAction.getRightValue(), intersectedAction.getRightValue());
	}

	@Test
	public void intersect_AnyValueSet() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new AnyValueSet()));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));
		Action outputAction = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));

		// When
		ElementaryCondition intersected =  action1.intersect(action2); 
		Action intersectedAction = (Action)intersected;

		// Then
		assertEquals(outputAction.getLeftValue(), intersectedAction.getLeftValue());
		assertEquals(outputAction.getRightValue(), intersectedAction.getRightValue());
	}

	@Test
	public void intersect_AnyValueSetReversed() {
		// Given
		Action action1 = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new AnyValueSet()));
		Action action2 = new Action(
				new ElementaryCondition("a", new Interval(1.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));
		Action outputAction = new Action(
				new ElementaryCondition("a", new Interval(3.5, Interval.INF, true, false)), 
				new ElementaryCondition("a", new Interval(9.5, Interval.INF, true, false)));

		// When
		ElementaryCondition intersected =  action2.intersect(action1); // action nb 2 makes intersection
		Action intersectedAction = (Action)intersected;

		// Then
		assertEquals(outputAction.getLeftValue(), intersectedAction.getLeftValue());
		assertEquals(outputAction.getRightValue(), intersectedAction.getRightValue());
	}
}
