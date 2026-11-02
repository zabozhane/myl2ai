import unittest

from movement_report import audit


def line(time, text):
    return f'game-1 | 2026-11-01T{time}Z {text}'


def status(time, **changes):
    fields = dict(intention='IDLE', traveling='false', chores='false', life='false',
                  autofarming='false', dead='false', sitting='false')
    fields.update(changes)
    return line(time, 'Offline farm status: Zenthen id=1 lv 10 exp 100 sp 0 at -10,20 '
                + ' '.join(f'{k}={v}' for k, v in fields.items()))


class MovementReportTest(unittest.TestCase):
    def test_refusals_group_by_actor_and_destination_without_inflating_trips(self):
        report = audit([
            line('19:00:00.100', 'Autobots route: A id=1 refused unsafe leg to -1,2,-3'),
            line('19:00:00.500', 'Autobots route: A id=1 refused unsafe leg to -1,2,-3'),
            line('19:00:01', 'Autobots route: B id=2 refused unsafe leg to -1,2,-3'),
            line('19:02:00', 'Autobots route: A id=1 refused unsafe leg to -1,2,-3'),
        ])
        group = report['groups'][0]
        self.assertEqual((group['lines'], group['bursts'], len(group['actors'])), (4, 3, 2))

    def test_idle_and_stationary_travel_are_observations(self):
        report = audit([status('19:00:00'), status('19:01:00'),
                        status('19:02:00', traveling='true', intention='MOVE_TO'),
                        status('19:03:00', traveling='true', intention='MOVE_TO')])
        self.assertEqual({r['kind'] for r in report['idle_observations']},
                         {'idle_without_action', 'stationary_travel'})

    def test_sitting_chores_death_and_large_sampling_gaps_are_not_stalls(self):
        for changes in ({'sitting': 'true'}, {'chores': 'true'}, {'dead': 'true'}, {'life': 'true'}):
            self.assertFalse(audit([status('19:00:00', **changes), status('19:01:00', **changes)])['idle_observations'])
        self.assertFalse(audit([status('19:00:00'), status('19:05:00')])['idle_observations'])

    def test_restart_separates_statuses_and_preserves_earlier_observations(self):
        result = audit([status('19:00:00'), status('19:01:00'),
                        line('19:01:01', 'Server build: revision=x'), status('19:01:30')])
        self.assertEqual(len(result['idle_observations']), 1)
        self.assertEqual(result['idle_observations'][0]['seconds'], 60)

    def test_new_failure_reason_and_destination_are_reported(self):
        report = audit([line('19:00:00', 'Autobots movement: A id=1 failed reason=no_safe_path to=-10,20,-30 at=1,2,3 elapsedMs=10')])
        self.assertEqual(report['counts'], {'route_no_safe_path': 1})
        self.assertEqual(report['groups'][0]['destination'], (-10, 20, -30))

    def test_silent_population_hunter_is_visible_but_declared_breaks_are_excluded(self):
        def population(time, **fields):
            return status(time, activity='Hunt', sequence='-', target='-', autofarming='true', **fields).replace('Offline farm status:', 'Autobots state:')
        report = audit([population('19:00:00'), population('19:01:00')])
        self.assertEqual(len(report['idle_observations']), 1)
        for activity in ('Afk', 'Home', 'Trade'):
            rows = [population('19:00:00').replace('activity=Hunt', 'activity='+activity),
                    population('19:01:00').replace('activity=Hunt', 'activity='+activity)]
            self.assertFalse(audit(rows)['idle_observations'])
        self.assertFalse(audit([population('19:00:00', store='true'), population('19:01:00', store='true')])['idle_observations'])


if __name__ == '__main__':
    unittest.main()
