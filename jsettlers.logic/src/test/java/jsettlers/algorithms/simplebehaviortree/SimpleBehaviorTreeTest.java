package jsettlers.algorithms.simplebehaviortree;

import org.junit.Test;

import jsettlers.algorithms.simplebehaviortree.nodes.Parallel;
import jsettlers.common.utils.mutables.MutableInt;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;
import static jsettlers.algorithms.simplebehaviortree.NodeStatus.*;
import static org.junit.Assert.assertEquals;

public class SimpleBehaviorTreeTest {

	@Test
	public void testSequence() {
		Root<TestMovable> seqTree = new Root<>(
				sequence(
						action(TestMovable::actionA),
						waitFor(condition(TestMovable::waitFunc)),
						action(TestMovable::actionB),
						action(TestMovable::actionC),
						action(TestMovable::reset)
				)
		);

		testTree(seqTree, RUNNING, SUCCESS, RUNNING, SUCCESS);
	}

	@Test
	public void testWait() {
		Root<TestMovable> seqTree = new Root<>(
				waitFor(condition(TestMovable::waitFunc))
		);

		testTree(seqTree, RUNNING, SUCCESS);
	}

	@Test
	public void testParallel() {
		Root<TestMovable> parallelTree = new Root<>(
				parallel(Parallel.Policy.ALL, false,
						alwaysSucceed(),
						waitFor(condition(TestMovable::waitFunc))
				)
		);

		Root<TestMovable> parallelTree2 = new Root<>(
				parallel(Parallel.Policy.ONE, false,
						alwaysSucceed(),
						alwaysFail(),
						waitFor(condition(TestMovable::waitFunc))
				)
		);

		Root<TestMovable> parallelTree3 = new Root<>(
				parallel(Parallel.Policy.ALL, false,
						alwaysSucceed(),
						alwaysFail(),
						waitFor(condition(TestMovable::waitFunc))
				)
		);

		Root<TestMovable> parallelTree4 = new Root<>(
				parallel(Parallel.Policy.ONE, true,
						alwaysSucceed(),
						alwaysFail(),
						waitFor(condition(TestMovable::waitFunc))
				)
		);
		Root<TestMovable> parallelTree5 = new Root<>(
				parallel(Parallel.Policy.ALL, true,
						alwaysSucceed(),
						alwaysFail(),
						waitFor(condition(TestMovable::waitFunc))
				)
		);

		testTree(parallelTree, RUNNING, SUCCESS);
		testTree(parallelTree2, RUNNING, SUCCESS);
		testTree(parallelTree3, RUNNING, FAILURE);
		testTree(parallelTree4, SUCCESS, SUCCESS);
		testTree(parallelTree5, RUNNING, FAILURE);
	}

	@Test
	public void testGuard() {
		Root<TestMovable> guardTree = new Root<>(
				guard(TestMovable::cond, alwaysSucceed())
		);

		testTree(guardTree, SUCCESS, FAILURE);
	}

	@Test(expected = AssertionError.class)
	public void testNot() {
		Root<TestMovable> notTree = new Root<>(
				sequence(
						action(TestMovable::actionB),
						action(TestMovable::actionA),
						action(TestMovable::actionC)
				)
		);
		testTree(notTree, SUCCESS);
	}

	public void testTree(Root<TestMovable> tree, NodeStatus... statuses) {
		testTree(new TestMovable(), tree, statuses);
	}

	public <T> void testTree(T target, Root<T> tree, NodeStatus... statuses) {
		Tick<T> tick = new Tick<>(target, tree);

		for(NodeStatus status : statuses) {
			assertEquals(status, tick.tick());
		}
	}

	@Test
	public void testRepeatCount() {
		Root<TestMovable> repeatTree = new Root<>(
				repeatLoop(2,
					sequence(
						action(TestMovable::actionA),
						alwaysFail()
					)
				)
		);

		testTree(repeatTree, FAILURE);

		Root<TestMovable> repeatTree2 = new Root<>(
				repeatLoop(1,
						action(TestMovable::actionA)
				)
		);

		testTree(repeatTree2, SUCCESS);


		MutableInt counter = new MutableInt();

		Root<TestMovable> repeatTree3 = new Root<>(
				repeatLoop(mov -> counter.value,
					action(mov -> {
						counter.value--;
					})
				)
		);

		for(int var : new int[] {0, 1, 2, 3, 4, 5, 100, 5000}) {
			counter.value = var;
			testTree(repeatTree3, SUCCESS);
			assertEquals(0, counter.value);
		}
	}

	@Test(expected = AssertionError.class)
	public void testRepeatCountFail() {
		Root<TestMovable> repeatTree = new Root<>(
				repeatLoop(2,
					action(TestMovable::actionA)
				)
		);

		testTree(repeatTree, FAILURE);
	}

	private class TestMovable {

		int i = 0;

		void actionA() {
			assertEquals(0, i);
			i++;
		}

		void actionB() {
			assertEquals(1, i);
			i++;

		}

		void actionC() {
			assertEquals(2, i);
			i++;

		}

		void reset() {
			assertEquals(3, i);
			i = 0;
			wait = true;
			cond = false;
		}

		boolean wait = true;

		boolean waitFunc() {
			if(wait) {
				wait = false;
				return false;
			}
			return true;
		}

		boolean cond = false;

		boolean cond() {
			if(!cond) {
				cond = true;
				return true;
			}
			return false;
		}
	}
}
