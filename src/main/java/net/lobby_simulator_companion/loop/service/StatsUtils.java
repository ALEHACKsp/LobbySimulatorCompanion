package net.lobby_simulator_companion.loop.service;

import java.util.Collection;

import static java.util.Comparator.reverseOrder;

/**
 * <pre>
 * The purpose is to have a measure of how good is the game map variation that we get, so we'll consider a
 * discrete uniform distribution [1].
 *
 * The input is going to be a vector where each component represents a distinct map and, its value, the number
 * of matches played on that map. Because the order of the distribution does not matter, we will sort this vector in
 * descending order.
 * For example, if we had 5 maps and a total of 8 matches played, we consider that this will be the ideal distribution
 * (100% variation): {2, 2, 2, 1, 1}. The worst-case distribution (0% variation) would be one where all the matches
 * have been played on a single map, like so: {8, 0, 0, 0, 0}. Between those two extremes, there is a series of possible
 * vectors, each with their own variation value. So, basically, what we are trying to do is writing the different
 * ways of achieving a sum, where the sum is the total number of matches and each component of the sum is the number
 * of matches on a specific map. This is called "partitions" in number theory [2] and, in particular, it's a special
 * case where the number of partitions (number of maps) is limited.
 * We can calculate the maximum number of partitions with this recursive function [3]:
 * p(k, n) = p(k, n-k) + p(k-1, n-1)
 * With k=5 and n=8 there's going to be 18 possible distributions [4]. Here's the list sorted (approximately) from
 * best to worst:
 * - {2, 2, 2, 1, 1}
 * - {2, 2, 2, 2, 0}
 * - {3, 2, 1, 1, 1}
 * - {3, 2, 2, 1, 0}
 * - {3, 3, 1, 1, 0}
 * - {3, 3, 2, 0, 0}
 * - {4, 1, 1, 1, 1}
 * - {4, 2, 1, 1, 0}
 * - {4, 2, 2, 0, 0}
 * - {4, 3, 1, 0, 0}
 * - {4, 4, 0, 0, 0}
 * - {5, 1, 1, 1, 0} <== look at this case. This distro could be considered better than the previous one
 * - {5, 2, 1, 0, 0}
 * - {5, 3, 0, 0, 0}
 * - {6, 1, 1, 0, 0}
 * - {6, 2, 0, 0, 0}
 * - {7, 1, 0, 0, 0}
 * - {8, 0, 0, 0, 0}
 *
 * We could use variants of the recursive function to calculate the rank/index of a given combination in lexicographic
 * order [5] but it's computationally expensive and, even so, there are several cases (like pointed above) where
 * lexicographically may not be the exact order we are looking for.
 *
 * So, to simplify, we will establish a pattern for what defines better distributions:
 * 1) Starting with a lower number is better:
 * Because our input is guaranteed to be sorted in descending order and for its sum to equal n, a lower number means
 * that we can better spread the remaining sum with the rest of the components.
 * For example, with n=4 and k=3, {2, 1, 1} is better than {3, 1, 0}. Because we started with '2' in the first vector,
 * we still have a remaining sum of 2 that we can spread between the other 2 components, whereas in the second vector
 * we started with '3', which means that we only have a remaining sum of 1 to spread with the rest of the components,
 * which means that the third component will get nothing.
 * 2) A similar criteria applies for the rest of the components in relation to their previous component:
 * for the same component, a lower number is better, and since it's sorted by descending order, this number cannot be
 * higher than the previous component.
 * 3) Components are more relevant from left to right.
 *
 * With those things in mind, we will construct a function 'f' that calculates a score from a given distribution
 * vector 'd'.
 * Let 's(c)' be a function that calculates the score of a distribution component (count) on the i-th index. Then:
 * f(d) = sum(s(d[i])
 * s(c) = ((k - i) / k) * (1 - d[i] / d[i - 1])
 *        --> the first factor weighs the position of the component;
 *            the second factor gets higher as the component gets lower in relation to the previous component
 *
 * Once we have our 'f(d)' function, we can calculate any distribution "goodness" by rating it over the best-case
 * distribution.
 * The time complexity is O(1).
 *
 * Here's an example of applying that function to the distributions above:
 * [8, 0, 0, 0, 0] ==>   0.00%
 * [7, 1, 0, 0, 0] ==>  35.48%
 * [6, 2, 0, 0, 0] ==>  36.56%
 * [5, 3, 0, 0, 0] ==>  37.63%
 * [4, 4, 0, 0, 0] ==>  38.71%
 * [6, 1, 1, 0, 0] ==>  63.44%
 * [5, 2, 1, 0, 0] ==>  64.52%
 * [4, 3, 1, 0, 0] ==>  65.59%
 * [4, 2, 2, 0, 0] ==>  66.67%
 * [3, 3, 2, 0, 0] ==>  67.74%
 * [5, 1, 1, 1, 0] ==>  83.87%
 * [4, 2, 1, 1, 0] ==>  84.95%
 * [3, 3, 1, 1, 0] ==>  86.02%
 * [3, 2, 2, 1, 0] ==>  87.10%
 * [2, 2, 2, 2, 0] ==>  90.32%
 * [4, 1, 1, 1, 1] ==>  96.77%
 * [3, 2, 1, 1, 1] ==>  97.85%
 * [2, 2, 2, 1, 1] ==> 100.00%
 *
 *
 * Possible problems:
 * ------------------
 * A natural consequence of the function is that it values more those distributions that have more non-zero components,
 * which in some cases can be controversial. For example:
 * [3, 3, 2, 0, 0] ==> 67.74%
 * [5, 1, 1, 1, 0] ==> 83.87%
 * Is the second distro really (THAT) better than the first one? it spreads the sum towards more components (one more)
 * but at the expense of playing 5 times on the same map. The first one seems to spread more evenly, at the cost of
 * reaching one less component.
 *
 *
 *
 * Other evaluated strategies that seemed to produce worse results:
 * ----------------------------------------------------------------
 * 1) Calculating an "error" of how much a given distribution deviates from the best distribution (e.g., by adding
 * the difference between the components of the given distribution and the best one).
 * 2) Calculating n-dimensional Eucledian distance between the best distribution and given distribution.
 *
 *
 * References:
 * [1]: https://en.wikipedia.org/wiki/Discrete_uniform_distribution
 * [2]: https://en.wikipedia.org/wiki/Partition_%28number_theory%29
 * [3]: https://math.stackexchange.com/a/217641
 * [4]: https://www.wolframalpha.com/input/?i=integer+partitions+of+8+with+size+%3C%3D+5
 * [5]: https://mathoverflow.net/a/145186
 * </pre>
 *
 * @author NickyRamone
 */
public class StatsUtils {

    public static float rateDistribution(Collection<Integer> distro) {
        int n = distro.stream().reduce(0, (result, element) -> result += element);
        int k = distro.size();

        if (n <= 1) {
            return 1;
        }

        int[] bestDistro = generateBestDistribution(n, k);
        int[] currentDistro = distro.stream()
                .sorted(reverseOrder())
                .mapToInt(i -> i)
                .toArray();

        double bestScore = calculateDistributionScore(bestDistro, n);
        double currentScore = calculateDistributionScore(currentDistro, n);

        return (float) (currentScore / bestScore);
    }

    private static int[] generateBestDistribution(int n, int k) {
        int[] d = new int[k];

        for (int i = 0, m = k; i < k; i++, m--) {
            int di = (int) Math.ceil((double) n / m);
            d[i] = di;
            n -= di;
        }

        return d;
    }

    private static double calculateDistributionScore(int[] d, int n) {
        double score = 0;

        for (int i = 0, k = d.length; i < k; i++) {
            score += d[i] == 0 ?
                    0 :
                    ((double) (k - i) / k) * (1 - d[i] / (double) n);
        }

        return score;
    }
}
